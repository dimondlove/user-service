package ui;

import exceptions.DaoException;
import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserService;
import utils.UserValidator;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

public class ConsoleMenu {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMenu.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Set<String> YES = Set.of("y", "yes", "д", "да");

    private final UserService userService;
    private final Scanner scanner;

    public ConsoleMenu(UserService userService) {
        this.userService = userService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        log.info("Консольное меню запущено");
        try {
            boolean running = true;
            while (running) {
                printMenu();
                running = handle(readLine("Выберите пункт: "));
            }
        } catch (InputEndedException e) {
            log.info("Ввод закрыт, завершаем работу");
        }
        System.out.println("До свидания!");
        log.info("Консольное меню завершено");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== User Service ==========");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Найти пользователя по ID");
        System.out.println("3. Показать всех пользователей");
        System.out.println("4. Обновить пользователя");
        System.out.println("5. Удалить пользователя");
        System.out.println("0. Выход");
        System.out.println("==================================");
    }

    private boolean handle(String choice) {
        log.debug("Выбран пункт меню: '{}'", choice);
        try {
            switch (choice) {
                case "1" -> createUser();
                case "2" -> findUser();
                case "3" -> showAllUsers();
                case "4" -> updateUser();
                case "5" -> deleteUser();
                case "0" -> {
                    return false;
                }
                default -> System.out.println("Неизвестный пункт меню, введите число от 0 до 5");
            }
        } catch (InputEndedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Некорректные данные: {}", e.getMessage());
            System.out.println("Ошибка ввода: " + e.getMessage());
        } catch (DaoException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Непредвиденная ошибка", e);
            System.out.println("Непредвиденная ошибка, подробности в logs/user-service.log");
        }
        return true;
    }

    private void createUser() {
        System.out.println("--- Создание пользователя ---");
        String name = readValid("Имя: ", UserValidator::validateName);
        String email = readValid("Email: ", UserValidator::validateEmail);
        int age = readValid("Возраст: ", UserValidator::parseAge);

        User user = new User(name, email, age);
        userService.saveUser(user);
        System.out.println("Пользователь создан: " + describe(user));
    }

    private void findUser() {
        System.out.println("--- Поиск пользователя ---");
        Long id = readValid("ID пользователя: ", UserValidator::parseId);
        Optional<User> user = userService.findUserById(id);
        if (user.isPresent()) {
            printUsers(List.of(user.get()));
        } else {
            System.out.println("Пользователь с id=" + id + " не найден");
        }
    }

    private void showAllUsers() {
        System.out.println("--- Все пользователи ---");
        List<User> users = userService.findAllUser();
        if (users.isEmpty()) {
            System.out.println("Пользователей пока нет");
        } else {
            printUsers(users);
            System.out.println("Всего: " + users.size());
        }
    }

    private void updateUser() {
        System.out.println("--- Обновление пользователя ---");
        Long id = readValid("ID пользователя: ", UserValidator::parseId);
        Optional<User> found = userService.findUserById(id);
        if (found.isEmpty()) {
            System.out.println("Пользователь с id=" + id + " не найден");
            return;
        }

        User user = found.get();
        System.out.println("Текущие данные: " + describe(user));
        System.out.println("(нажмите Enter, чтобы оставить значение без изменений)");

        user.setName(readValidOrKeep("Имя [" + user.getName() + "]: ", user.getName(), UserValidator::validateName));
        user.setEmail(readValidOrKeep("Email [" + user.getEmail() + "]: ", user.getEmail(), UserValidator::validateEmail));
        user.setAge(readValidOrKeep("Возраст [" + user.getAge() + "]: ", user.getAge(), UserValidator::parseAge));

        userService.updateUser(user);
        System.out.println("Пользователь обновлён: " + describe(user));
    }

    private void deleteUser() {
        System.out.println("--- Удаление пользователя ---");
        Long id = readValid("ID пользователя: ", UserValidator::parseId);
        Optional<User> found = userService.findUserById(id);
        if (found.isEmpty()) {
            System.out.println("Пользователь с id=" + id + " не найден");
            return;
        }

        User user = found.get();
        System.out.println("Будет удалён: " + describe(user));
        if (YES.contains(readLine("Подтвердите удаление (y/n): ").toLowerCase())) {
            userService.deleteUser(user);
            System.out.println("Пользователь удалён");
        } else {
            System.out.println("Удаление отменено");
            log.debug("Удаление пользователя id={} отменено пользователем", id);
        }
    }

    private void printUsers(List<User> users) {
        String format = "%-5s %-20s %-30s %-8s %-16s%n";
        System.out.printf(format, "ID", "Имя", "Email", "Возраст", "Создан");
        System.out.println("-".repeat(83));
        for (User u : users) {
            System.out.printf(format,
                    u.getId(),
                    cut(u.getName(), 20),
                    cut(u.getEmail(), 30),
                    u.getAge(),
                    u.getCreatedAt() == null ? "-" : u.getCreatedAt().format(DATE_FORMAT));
        }
    }

    private String describe(User u) {
        return "id=" + u.getId() + ", имя=" + u.getName() + ", email=" + u.getEmail() + ", возраст=" + u.getAge();
    }

    private String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputEndedException();
        }
        return scanner.nextLine().trim();
    }

    private <T> T readValid(String prompt, Function<String, T> parser) {
        while (true) {
            try {
                return parser.apply(readLine(prompt));
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private <T> T readValidOrKeep(String prompt, T current, Function<String, T> parser) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return current;
            }
            try {
                return parser.apply(line);
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private static final class InputEndedException extends RuntimeException {
    }
}
