package dao;

import exceptions.DaoException;
import models.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты DAO в Docker-контейнере (Testcontainers).
 */
@Testcontainers
class UserDaoImplIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine").withInitScript("init.sql");

    private static SessionFactory sessionFactory;

    private UserDao userDao;

    @BeforeAll
    static void createSessionFactory() {
        sessionFactory = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl())
                .setProperty("hibernate.connection.username", POSTGRES.getUsername())
                .setProperty("hibernate.connection.password", POSTGRES.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .addAnnotatedClass(User.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void cleanDatabase() {
        sessionFactory.inTransaction(session ->
                session.createMutationQuery("delete from User").executeUpdate());
        userDao = new UserDaoImpl(sessionFactory);
    }

    // save

    @Test
    @DisplayName("save: присваивает id и время создания")
    void save_assignsIdAndCreatedAt() {
        User user = new User("Иван", "ivan@mail.ru", 25);

        userDao.save(user);

        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("save: данные сохраняются в БД и читаются обратно")
    void save_persistsData() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);

        User found = userDao.findById(user.getId()).orElseThrow();

        assertEquals("Иван", found.getName());
        assertEquals("ivan@mail.ru", found.getEmail());
        assertEquals(25, found.getAge());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    @DisplayName("save: дубликат email -> DaoException, в БД остаётся одна запись")
    void save_duplicateEmail_throwsDaoException() {
        userDao.save(new User("Иван", "same@mail.ru", 25));

        DaoException ex = assertThrows(DaoException.class,
                () -> userDao.save(new User("Пётр", "same@mail.ru", 30)));

        assertTrue(ex.getMessage().contains("email"), "сообщение должно упоминать email: " + ex.getMessage());
        List<User> all = userDao.findAll();
        assertEquals(1, all.size());
        assertEquals("Иван", all.get(0).getName());
    }

    // findById

    @Test
    @DisplayName("findById: возвращает пустой Optional для несуществующего id")
    void findById_unknownId_returnsEmpty() {
        Optional<User> result = userDao.findById(999_999L);

        assertTrue(result.isEmpty());
    }

    // findAll

    @Test
    @DisplayName("findAll: пустая таблица -> пустой список")
    void findAll_emptyTable_returnsEmptyList() {
        assertTrue(userDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll: возвращает всех пользователей, отсортированных по id")
    void findAll_returnsAllUsersOrderedById() {
        userDao.save(new User("Первый", "first@mail.ru", 20));
        userDao.save(new User("Второй", "second@mail.ru", 30));
        userDao.save(new User("Третий", "third@mail.ru", 40));

        List<User> all = userDao.findAll();

        assertEquals(List.of("Первый", "Второй", "Третий"), all.stream().map(User::getName).toList());
    }

    // update

    @Test
    @DisplayName("update: изменения сохраняются в БД")
    void update_persistsChanges() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);

        user.setName("Иван Петров");
        user.setEmail("ivan.petrov@mail.ru");
        user.setAge(26);
        userDao.update(user);

        User found = userDao.findById(user.getId()).orElseThrow();
        assertEquals("Иван Петров", found.getName());
        assertEquals("ivan.petrov@mail.ru", found.getEmail());
        assertEquals(26, found.getAge());
    }

    @Test
    @DisplayName("update: время создания не меняется")
    void update_doesNotChangeCreatedAt() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);
        var createdAtBefore = userDao.findById(user.getId()).orElseThrow().getCreatedAt();

        user.setName("Новое имя");
        userDao.update(user);

        var createdAtAfter = userDao.findById(user.getId()).orElseThrow().getCreatedAt();
        assertEquals(createdAtBefore, createdAtAfter);
    }

    @Test
    @DisplayName("update: не затрагивает других пользователей")
    void update_doesNotAffectOtherUsers() {
        User first = new User("Первый", "first@mail.ru", 20);
        User second = new User("Второй", "second@mail.ru", 30);
        userDao.save(first);
        userDao.save(second);

        first.setName("Изменённый");
        userDao.update(first);

        assertEquals("Второй", userDao.findById(second.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("update: несуществующий пользователь -> DaoException")
    void update_nonExistentUser_throwsDaoException() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);
        userDao.delete(user); // у объекта остаётся id, но записи в БД уже нет

        user.setName("Призрак");
        DaoException ex = assertThrows(DaoException.class, () -> userDao.update(user));

        assertTrue(ex.getMessage().contains("не найден"));
        assertTrue(userDao.findAll().isEmpty(), "update не должен был создать запись");
    }

    @Test
    @DisplayName("update: email другого пользователя -> DaoException, данные в БД не меняются")
    void update_duplicateEmail_throwsDaoExceptionAndRollsBack() {
        User first = new User("Первый", "first@mail.ru", 20);
        User second = new User("Второй", "second@mail.ru", 30);
        userDao.save(first);
        userDao.save(second);

        second.setEmail("first@mail.ru");
        assertThrows(DaoException.class, () -> userDao.update(second));

        assertEquals("second@mail.ru", userDao.findById(second.getId()).orElseThrow().getEmail());
    }

    // delete

    @Test
    @DisplayName("delete: пользователь удаляется из БД")
    void delete_removesUser() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);

        userDao.delete(user);

        assertTrue(userDao.findById(user.getId()).isEmpty());
        assertTrue(userDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("delete: удаляет только указанного пользователя")
    void delete_removesOnlyGivenUser() {
        User first = new User("Первый", "first@mail.ru", 20);
        User second = new User("Второй", "second@mail.ru", 30);
        userDao.save(first);
        userDao.save(second);

        userDao.delete(first);

        List<User> rest = userDao.findAll();
        assertEquals(1, rest.size());
        assertEquals("Второй", rest.get(0).getName());
    }

    @Test
    @DisplayName("delete: повторное удаление -> DaoException")
    void delete_alreadyDeletedUser_throwsDaoException() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);
        userDao.delete(user);

        DaoException ex = assertThrows(DaoException.class, () -> userDao.delete(user));

        assertTrue(ex.getMessage().contains("не найден"));
    }

    // изоляция

    @Test
    @DisplayName("изоляция: после удаления email можно использовать повторно")
    void save_afterDelete_emailCanBeReused() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        userDao.save(user);
        userDao.delete(user);

        User again = new User("Иван Второй", "ivan@mail.ru", 25);
        userDao.save(again);

        assertEquals(1, userDao.findAll().size());
    }
}
