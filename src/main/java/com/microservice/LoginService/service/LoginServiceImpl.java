package com.microservice.LoginService.service;

import com.microservice.LoginService.dao.LoginDao;
import com.microservice.LoginService.model.Login;
import com.microservice.LoginService.repository.LoginRepository;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoginServiceImpl implements ILoginService{
    @Autowired
    LoginRepository repository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JWTService jwtService;


//    @PostConstruct
//    void init(){
//        LoginDao loginDao = new LoginDao();
//        loginDao.setUserName("Rajat");
//        loginDao.setPassword(new BCryptPasswordEncoder().encode("Password"));
//        repository.save(loginDao);
//    }

    @Override
    public boolean createUser(String userName, String password) {
        LoginDao loginDao = new LoginDao();
        loginDao.setUserName(userName);
        loginDao.setPassword(new BCryptPasswordEncoder().encode(password));
       try {
           repository.save(loginDao);
       } catch (Exception e) {
           return false;
       }
        return true;
    }

    @Override
    public String userLogin(String userName, String password) {
        //Optional<LoginDao> dao = repository.findByUserNameAndPassword(userName, new BCryptPasswordEncoder().encode(password));
        Authentication auth = authenticationManager.authenticate
                (new UsernamePasswordAuthenticationToken(userName, password));
        if(auth.isAuthenticated()){
            return jwtService.generateToken(userName);
        } else {
            return "Invalid Credentials";
        }
    }

    @Override
    public List<Login> getAllUsers(){
        List<Login> users = new ArrayList<>();
        repository.findAll().forEach(item ->{
            Login login = new Login();
            login.setUserName(item.getUserName());
            login.setPassword(item.getPassword());
            users.add(login);
        });
        return users;
    }
}
