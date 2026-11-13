package dao;

import models.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    public Optional<User> findById(Long id);

    public void save(User user);

    public void update(User user);

    public void delete(User user);

    public List<User> findAll();
}
