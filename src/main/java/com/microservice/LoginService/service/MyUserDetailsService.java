package com.microservice.LoginService.service;

import com.microservice.LoginService.dao.LoginDao;
import com.microservice.LoginService.model.Login;
import com.microservice.LoginService.model.MyUserDetails;
import com.microservice.LoginService.repository.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    LoginRepository repository;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<LoginDao> loginDao = repository.findByUserName(username);
        if(loginDao.isEmpty()){
            throw new UsernameNotFoundException("User Not found");
        }else {
            Login login = new Login();
            login.setUserName(loginDao.get().getUserName());
            login.setPassword(loginDao.get().getPassword());
            return new MyUserDetails(login);
        }
    }
}
