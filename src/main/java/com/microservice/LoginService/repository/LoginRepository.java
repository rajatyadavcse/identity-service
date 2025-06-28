package com.microservice.LoginService.repository;

import com.microservice.LoginService.dao.LoginDao;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LoginRepository extends CrudRepository<LoginDao, Long> {
    Optional<LoginDao> findByUserNameAndPassword(String username, String password);
    Optional<LoginDao> findByUserName(String username);
}
