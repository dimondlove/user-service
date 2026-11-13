package dao;

import exceptions.DaoException;
import jakarta.persistence.PersistenceException;
import models.User;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import utils.HibernateSessionFactoryUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class UserDaoImpl implements UserDao {

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(execute("поиск пользователя id=" + id, session ->
                session.find(User.class, id)));
    }

    @Override
    public void save(User user) {
        executeVoid("сохранение пользователя", session -> session.persist(user));
    }

    @Override
    public void update(User user) {
        executeVoid("обновление пользователя id=" + user.getId(), session -> {
            if (session.find(User.class, user.getId()) == null) {
                throw new DaoException("Пользователь с id=" + user.getId() + " не найден");
            }
            session.merge(user);
        });
    }

    @Override
    public void delete(User user) {
        executeVoid("удаление пользователя id=" + user.getId(), session -> {
            User managed = session.find(User.class, user.getId());
            if (managed == null) {
                throw new DaoException("Пользователь с id=" + user.getId() + " не найден");
            }
            session.remove(managed);
        });
    }

    @SuppressWarnings("JpaQlInspection")
    @Override
    public List<User> findAll() {
        return execute("получение списка пользователей", session ->
            session.createSelectionQuery("from User order by id", User.class).getResultList());

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
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException) {
                return new DaoException("Нарушено ограничение БД (возможно, email уже существует) при операции: " + action, ex);
            }
        }
        return new DaoException("Ошибка при работе с БД: " + action, ex);
    }
}
