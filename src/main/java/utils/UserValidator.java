package utils;

import models.User;

import java.util.regex.Pattern;

public final class UserValidator {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 150;
    public static final int MIN_AGE = 0;
    public static final int MAX_AGE = 150;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private UserValidator() {
    }

    public static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Имя слишком длинное (максимум " + MAX_NAME_LENGTH + " символов)");
        }
        return trimmed;
    }

    public static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email слишком длинный (максимум " + MAX_EMAIL_LENGTH + " символов)");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Некорректный формат email (пример: name@example.com)");
        }
        return normalized;
    }

    public static int validateAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException("Возраст должен быть от " + MIN_AGE + " до " + MAX_AGE);
        }
        return age;
    }

    public static int parseAge(String text) {
        try {
            return validateAge(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Возраст должен быть целым числом");
        }
    }

    public static Long parseId(String text) {
        try {
            long id = Long.parseLong(text.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("ID должен быть положительным числом");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID должен быть целым числом");
        }
    }

    public static void validate(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не задан");
        }
        user.setName(validateName(user.getName()));
        user.setEmail(validateEmail(user.getEmail()));
        validateAge(user.getAge());
    }
}
