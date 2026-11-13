import models.User;
import service.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        UserService userService = new UserService();

        User user = new User("Ivan", "ivan@ya.ru", 25, Instant.now());
        userService.saveUser(user);
        Optional<User> optionalUser = userService.findUserById(user.getId());
        User modUser = new User();
        if (optionalUser.isPresent()) {
            modUser = optionalUser.get();
        }
        System.out.println(modUser);
        modUser.setEmail("ivan2@ya.ru");
        System.out.println(modUser);
        userService.updateUser(modUser);
        Optional<User> optionalUser2 = userService.findUserById(user.getId());
        optionalUser2.ifPresent(System.out::println);
        User user2 = new User("Petya", "petya@ya.ru", 15, Instant.now());
        userService.saveUser(user2);
        List<User> users = userService.findAllUser();
        System.out.println(users);
        userService.deleteUser(modUser);
    }
}
