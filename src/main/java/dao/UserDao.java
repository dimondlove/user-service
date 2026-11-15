package dao;

import models.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(Long id);

    void save(User user);

    void update(User user);

    void delete(User user);

    List<User> findAll();
}
