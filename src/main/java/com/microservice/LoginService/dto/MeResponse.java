package com.microservice.LoginService.dto;

import com.microservice.LoginService.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private String username;
    private String role;
    private Long restaurantId;

    public static MeResponse from(User user) {
        return MeResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role("ROLE_" + user.getRole().name())
                .restaurantId(user.getRestaurantId())
                .build();
    }
}
