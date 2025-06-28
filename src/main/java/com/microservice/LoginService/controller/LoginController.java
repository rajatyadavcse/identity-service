package com.microservice.LoginService.controller;

import com.microservice.LoginService.model.Login;
import com.microservice.LoginService.service.ILoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class LoginController {
    @Autowired
    ILoginService loginService;

    @PostMapping("/create")
    public String createUser(@RequestBody Login request){
        if(null == request.getUserName()){
            return  "Username is required field";
        }
        if(null == request.getPassword()){
            return "Password is a required field";
        }
        boolean result = loginService.createUser(request.getUserName(), request.getPassword());
        return result ? "User successfully created!" : "Unable to create user";
    }

    @PostMapping("/signin")
    public String userLogin(@RequestBody Login request){
        if(null == request.getUserName()){
            return  "Username is required field";
        }
        if(null == request.getPassword()){
            return "Password is a required field";
        }
        return loginService.userLogin(request.getUserName(), request.getUserName());
    }

    @GetMapping("/getInfo")
    public List<Login> getInfo(HttpServletRequest request) {
        return loginService.getAllUsers();
    }

    @GetMapping("/getUserByUserName/{userName}")
    public Login getUserByUserName(@PathVariable(name = "userName") String userName, @RequestParam(required = false) String hello,
                                   @RequestParam(required = false) String bye){

        System.out.println("hello"+hello);
        System.out.println("bye"+bye);
        System.out.println("UserName:"+userName);
        return  new Login();
    }
}
