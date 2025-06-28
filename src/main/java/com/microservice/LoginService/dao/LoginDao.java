package com.microservice.LoginService.dao;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "login")
public class LoginDao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String userName;

    @Column
    private String password;
}
