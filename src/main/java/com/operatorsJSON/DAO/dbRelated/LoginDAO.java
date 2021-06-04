package com.operatorsJSON.DAO.dbRelated;

import com.operatorsJSON.beans.dbRelated.Login;
import com.operatorsJSON.repo.dbRelated.LoginRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class LoginDAO {
    @Autowired
    LoginRepo loginRepo;

    public void addLogin(Login login) {
        loginRepo.save(login);
    }

    public void deleteLogin(Login login) {
        loginRepo.delete(login);
    }

    public Collection<Login> getAllLogins() {
        return loginRepo.findAll();
    }

    public Optional<Login> findLoginByUserName(String userName) {
        return loginRepo.findByUserNameIgnoreCase(userName);
    }

    public Optional<Login> findLoginById(long id) {
        return loginRepo.findById(id);
    }
}
