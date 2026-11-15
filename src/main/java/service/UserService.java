package service;

import dao.UserDao;
import dao.UserDaoImpl;
import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.UserValidator;

import java.util.List;
import java.util.Optional;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService() {
        this(new UserDaoImpl());
    }

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> findUserById(Long id){
        log.debug("findUserById({})", id);
        return userDao.findById(id);
    }

    public void saveUser(User user){
        log.debug("saveUser({})", user);
        UserValidator.validate(user);
        userDao.save(user);
    }

    public void deleteUser(User user){
        log.debug("deleteUser({})", user);
        userDao.delete(user);
    }

    public void updateUser(User user){
        log.debug("updateUser({})", user);
        UserValidator.validate(user);
        userDao.update(user);
    }

    public List<User> findAllUser(){
        log.debug("findAllUser()");
        return userDao.findAll();
    }
}
