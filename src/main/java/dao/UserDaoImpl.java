package dao;

import exceptions.DaoException;
import jakarta.persistence.PersistenceException;
import models.User;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.HibernateSessionFactoryUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    /** Код ошибки PostgreSQL: нарушение уникальности. */
    private static final String PG_UNIQUE_VIOLATION = "23505";

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Поиск пользователя по id={}", id);
        Optional<User> result = Optional.ofNullable(execute("поиск пользователя id=" + id, session ->
                session.find(User.class, id)));
        log.debug("Результат поиска id={}: {}", id, result.isPresent() ? "найден" : "не найден");
        return result;
    }

    @Override
    public void save(User user) {
        log.debug("Сохранение пользователя: {}", user);
        executeVoid("сохранение пользователя", session -> session.persist(user));
        log.info("Пользователь сохранён: id={}, email={}", user.getId(), user.getEmail());
    }

    @Override
    public void update(User user) {
        log.debug("Обновление пользователя: {}", user);
        executeVoid("обновление пользователя id=" + user.getId(), session -> {
            if (session.find(User.class, user.getId()) == null) {
                log.warn("Обновление невозможно: пользователь id={} не найден", user.getId());
                throw new DaoException("Пользователь с id=" + user.getId() + " не найден");
            }
            session.merge(user);
        });
        log.info("Пользователь обновлён: id={}", user.getId());
    }

    @Override
    public void delete(User user) {
        log.debug("Удаление пользователя: {}", user);
        executeVoid("удаление пользователя id=" + user.getId(), session -> {
            User managed = session.find(User.class, user.getId());
            if (managed == null) {
                log.warn("Удаление невозможно: пользователь id={} не найден", user.getId());
                throw new DaoException("Пользователь с id=" + user.getId() + " не найден");
            }
            session.remove(managed);
        });
        log.info("Пользователь удалён: id={}", user.getId());
    }

    @SuppressWarnings("JpaQlInspection")
    @Override
    public List<User> findAll() {
        log.debug("Получение списка всех пользователей");
        List<User> users = execute("получение списка пользователей", session ->
            session.createSelectionQuery("from User order by id", User.class).getResultList());
        log.debug("Получено пользователей: {}", users.size());
        return users;
    }

    private <T> T execute(String action, Function<Session, T> work) {
        try {
            return HibernateSessionFactoryUtil.getSessionFactory().fromTransaction(work);
        } catch (PersistenceException ex) {
            throw translateException(action, ex);
        }
    }

    private void executeVoid(String action, Consumer<Session> work) {
        try {
            HibernateSessionFactoryUtil.getSessionFactory().inTransaction(work);
        } catch (PersistenceException ex) {
            throw translateException(action, ex);
        }
    }

    private DaoException translateException(String action, PersistenceException ex) {
        log.error("Ошибка БД при операции: {}", action, ex);

        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JDBCConnectionException) {
                return new DaoException("Нет соединения с БД. Проверьте, что контейнер запущен (docker compose up -d)", ex);
            }
            if (t instanceof ConstraintViolationException cve) {
                if (PG_UNIQUE_VIOLATION.equals(cve.getSQLState())) {
                    return new DaoException("Пользователь с таким email уже существует", ex);
                }
                return new DaoException("Нарушено ограничение БД при операции: " + action, ex);
            }
            if (t instanceof JDBCException) {
                return new DaoException("Ошибка SQL при операции: " + action, ex);
            }
        }
        return new DaoException("Ошибка при работе с БД: " + action, ex);
    }
}
