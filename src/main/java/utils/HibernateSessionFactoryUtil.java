package utils;

import exceptions.DaoException;
import models.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HibernateSessionFactoryUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateSessionFactoryUtil.class);

    private static SessionFactory sessionFactory;

    private  HibernateSessionFactoryUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                log.info("Создание SessionFactory");
                sessionFactory = new Configuration()
                        .configure()
                        .addAnnotatedClass(User.class)
                        .buildSessionFactory();
                log.info("SessionFactory создана");
            } catch (Exception ex) {
                log.error("Не удалось создать SessionFactory", ex);
                throw new DaoException("Не удалось инициализировать Hibernate. Проверьте, что БД запущена "
                        + "(docker compose up -d) и таблица users соответствует сущности", ex);
            }
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            try {
                if (sessionFactory.isOpen()) {
                    sessionFactory.close();
                    log.info("SessionFactory закрыта");
                }
            } catch (Exception ex) {
                log.warn("Ошибка при закрытии SessionFactory", ex);
            } finally {
                sessionFactory = null;
            }
        }
    }

}
