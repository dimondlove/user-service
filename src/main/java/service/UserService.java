package service;

import dao.UserDao;
import dao.UserDaoImpl;
import models.User;

import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao =  new UserDaoImpl();

    public UserService() {
    }

    public Optional<User> findUserById(Long id){
        return userDao.findById(id);
    }

    public void saveUser(User user){
        userDao.save(user);
    }

    public void deleteUser(User user){
        userDao.delete(user);
    }

    public void updateUser(User user){
        userDao.update(user);
    }

    public List<User> findAllUser(){
        return userDao.findAll();
    }
}
