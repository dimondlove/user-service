package service;

import dao.UserDao;
import exceptions.DaoException;
import models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты UserService: DAO замокан, БД не используется.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao);
    }

    // findUserById

    @Test
    @DisplayName("findUserById: возвращает пользователя, если он найден")
    void findUserById_whenExists_returnsUser() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findUserById(1L);

        assertTrue(result.isPresent());
        assertSame(user, result.get());
        verify(userDao).findById(1L);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("findUserById: возвращает пустой Optional, если пользователя нет")
    void findUserById_whenNotFound_returnsEmpty() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertTrue(userService.findUserById(99L).isEmpty());
        verify(userDao).findById(99L);
    }

    @Test
    @DisplayName("findUserById: пробрасывает DaoException из DAO")
    void findUserById_whenDaoFails_propagatesException() {
        when(userDao.findById(1L)).thenThrow(new DaoException("Нет соединения с БД"));

        DaoException ex = assertThrows(DaoException.class, () -> userService.findUserById(1L));
        assertEquals("Нет соединения с БД", ex.getMessage());
    }

    // findAllUser

    @Test
    @DisplayName("findAllUser: возвращает список из DAO")
    void findAllUser_returnsUsersFromDao() {
        List<User> users = List.of(
                new User("Иван", "ivan@mail.ru", 25),
                new User("Анна", "anna@mail.ru", 30));
        when(userDao.findAll()).thenReturn(users);

        assertEquals(users, userService.findAllUser());
        verify(userDao).findAll();
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("findAllUser: возвращает пустой список, если пользователей нет")
    void findAllUser_whenNoUsers_returnsEmptyList() {
        when(userDao.findAll()).thenReturn(List.of());

        assertTrue(userService.findAllUser().isEmpty());
    }

    // saveUser

    @Test
    @DisplayName("saveUser: нормализует имя и email и передаёт пользователя в DAO")
    void saveUser_validUser_normalizesAndCallsDao() {
        User user = new User("  Иван  ", "  IVAN@Mail.RU ", 25);

        userService.saveUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Иван", saved.getName());
        assertEquals("ivan@mail.ru", saved.getEmail());
        assertEquals(25, saved.getAge());
        verifyNoMoreInteractions(userDao);
    }

    @ParameterizedTest(name = "возраст {0} допустим")
    @ValueSource(ints = {0, 1, 150})
    @DisplayName("saveUser: граничные значения возраста принимаются")
    void saveUser_boundaryAge_isAccepted(int age) {
        userService.saveUser(new User("Иван", "ivan@mail.ru", age));

        verify(userDao).save(any(User.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUsers")
    @DisplayName("saveUser: некорректные данные отклоняются, DAO не вызывается")
    void saveUser_invalidUser_throwsAndDoesNotCallDao(String description, User user) {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(user));

        verifyNoInteractions(userDao);
    }

    @Test
    @DisplayName("saveUser(null): бросает IllegalArgumentException")
    void saveUser_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(null));

        verifyNoInteractions(userDao);
    }

    @Test
    @DisplayName("saveUser: пробрасывает DaoException из DAO")
    void saveUser_whenDaoFails_propagatesException() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        doThrow(new DaoException("Пользователь с таким email уже существует")).when(userDao).save(any(User.class));

        DaoException ex = assertThrows(DaoException.class, () -> userService.saveUser(user));
        assertEquals("Пользователь с таким email уже существует", ex.getMessage());
    }

    // updateUser

    @Test
    @DisplayName("updateUser: валидный пользователь передаётся в DAO")
    void updateUser_validUser_callsDao() {
        User user = new User("Иван", "IVAN@mail.ru", 26);

        userService.updateUser(user);

        verify(userDao).update(user);
        assertEquals("ivan@mail.ru", user.getEmail());
        verifyNoMoreInteractions(userDao);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUsers")
    @DisplayName("updateUser: некорректные данные отклоняются, DAO не вызывается")
    void updateUser_invalidUser_throwsAndDoesNotCallDao(String description, User user) {
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(user));

        verifyNoInteractions(userDao);
    }

    @Test
    @DisplayName("updateUser: пробрасывает DaoException из DAO")
    void updateUser_whenDaoFails_propagatesException() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        doThrow(new DaoException("Пользователь с id=1 не найден")).when(userDao).update(any(User.class));

        assertThrows(DaoException.class, () -> userService.updateUser(user));
    }

    // deleteUser

    @Test
    @DisplayName("deleteUser: передаёт пользователя в DAO")
    void deleteUser_callsDao() {
        User user = new User("Иван", "ivan@mail.ru", 25);

        userService.deleteUser(user);

        verify(userDao).delete(user);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("deleteUser: пробрасывает DaoException из DAO")
    void deleteUser_whenDaoFails_propagatesException() {
        User user = new User("Иван", "ivan@mail.ru", 25);
        doThrow(new DaoException("Пользователь с id=1 не найден")).when(userDao).delete(user);

        assertThrows(DaoException.class, () -> userService.deleteUser(user));
    }

    // данные для параметризованных тестов

    static Stream<Arguments> invalidUsers() {
        return Stream.of(
                Arguments.of("пустое имя", new User("", "a@b.ru", 20)),
                Arguments.of("имя из пробелов", new User("   ", "a@b.ru", 20)),
                Arguments.of("имя null", new User(null, "a@b.ru", 20)),
                Arguments.of("имя длиннее 100 символов", new User("а".repeat(101), "a@b.ru", 20)),
                Arguments.of("email null", new User("Иван", null, 20)),
                Arguments.of("пустой email", new User("Иван", "", 20)),
                Arguments.of("email без @", new User("Иван", "not-an-email", 20)),
                Arguments.of("email без домена верхнего уровня", new User("Иван", "ivan@mail", 20)),
                Arguments.of("email длиннее 150 символов", new User("Иван", "a".repeat(150) + "@b.ru", 20)),
                Arguments.of("отрицательный возраст", new User("Иван", "ivan@mail.ru", -1)),
                Arguments.of("возраст больше 150", new User("Иван", "ivan@mail.ru", 151))
        );
    }
}
