package com.microservice.LoginService.service;

import com.microservice.LoginService.dao.LoginDao;
import com.microservice.LoginService.model.Login;

import java.util.List;

public interface ILoginService {
    public boolean createUser(String userName, String password);
    public String userLogin(String userName, String password);
    public List<Login> getAllUsers();
}
