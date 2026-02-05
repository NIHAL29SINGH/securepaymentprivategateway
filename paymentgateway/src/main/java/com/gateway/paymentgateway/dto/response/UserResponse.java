package com.gateway.paymentgateway.dto.response;

import com.gateway.paymentgateway.entity.User;
import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private boolean active;

    public static UserResponse from(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setActive(user.isActive());
        return res;
    }
}
