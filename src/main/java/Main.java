import exceptions.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserService;
import ui.ConsoleMenu;
import utils.HibernateSessionFactoryUtil;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Запуск user-service");
        try {
            HibernateSessionFactoryUtil.getSessionFactory();
            new ConsoleMenu(new UserService()).run();
        } catch (DaoException e) {
            System.out.println("Не удалось запустить приложение: " + e.getMessage());
        } finally {
            HibernateSessionFactoryUtil.shutdown();
            log.info("user-service остановлен");
        }
    }
}
