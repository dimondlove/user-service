package utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Юнит-тесты валидатора */
class UserValidatorTest {

    // name

    @Test
    @DisplayName("validateName: обрезает пробелы по краям")
    void validateName_trimSpaces() {
        assertEquals("Анна", UserValidator.validateName(" Анна "));
    }

    @Test
    @DisplayName("validateName: имя из 100 символов допустимо")
    void validateName_maxLength_isAccepted() {
        String name = "а".repeat(100);
        assertEquals(name, UserValidator.validateName(name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("validateName: пустое имя отклоняется")
    void validateName_blank_throws(String name) {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.validateName(name));
    }

    @Test
    @DisplayName("validateName: имя из 101 символа отклоняется")
    void validateName_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.validateName("a".repeat(101)));
    }

    // email

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "first.last+tag@sub.domain.org", "a_b-c@mail.ru"})
    @DisplayName("validateEmail: корректные адреса принимаются")
    void validateEmail_valid_isAccepted(String email) {
        assertEquals(email, UserValidator.validateEmail(email));
    }

    @Test
    @DisplayName("validateEmail: приводит к нижнему регистру и обрезает пробелы")
    void validateEmail_normalizes() {
        assertEquals("ivan@mail.ru", UserValidator.validateEmail("  IVAN@Mail.RU "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "plain", "no-at.example.com", "@mail.ru", "ivan@", "ivan@mail", "iv an@mail.ru"})
    @DisplayName("validateEmail: некорректные адреса отклоняются")
    void validateEmail_invalid_throws(String email) {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.validateEmail(email));
    }

    // age

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 99, 150})
    @DisplayName("validateAge: значения 0..150 допустимы")
    void validateAge_valid_isAccepted(int age) {
        assertEquals(age, UserValidator.validateAge(age));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 151, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("validateAge: значения вне диапазона отклоняются")
    void validateAge_outOfRange_throws(int age) {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.validateAge(age));
    }

    @Test
    @DisplayName("parseAge: разбирает число с пробелами")
    void parseAge_parsesNumber() {
        assertEquals(25, UserValidator.parseAge(" 25 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "2.5", "-1", "151", "99999999999"})
    @DisplayName("parseAge: не число или вне диапазона отклоняется")
    void parseAge_invalid_throws(String text) {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.parseAge(text));
    }

    // id
    @Test
    @DisplayName("parseId: разбирает положительное число")
    void parseId_parsesNumber() {
        assertEquals(7L, UserValidator.parseId(" 7 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.5", "0", "-3"})
    @DisplayName("parseId: не число или не положительное отклоняется")
    void parseId_invalid_throws(String text) {
        assertThrows(IllegalArgumentException.class, () -> UserValidator.parseId(text));
    }

}
