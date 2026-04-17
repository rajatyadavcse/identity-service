package com.microservice.LoginService.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String phone;
    private String email;
    private Long restaurantId;
}
