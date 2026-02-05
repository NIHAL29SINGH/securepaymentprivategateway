package com.gateway.paymentgateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String name;
    private boolean emailVerified;
    private long transactionCount;
}
