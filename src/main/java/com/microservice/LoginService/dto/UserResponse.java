package com.microservice.LoginService.dto;

import com.microservice.LoginService.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private Long restaurantId;
    private String phone;
    private String email;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role("ROLE_" + user.getRole().name())
                .restaurantId(user.getRestaurantId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
