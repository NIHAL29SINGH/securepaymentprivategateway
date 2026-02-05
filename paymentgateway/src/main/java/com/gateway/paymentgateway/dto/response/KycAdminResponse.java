package com.gateway.paymentgateway.dto.response;

import com.gateway.paymentgateway.entity.KycStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycAdminResponse {

    private Long userId;
    private String email;
    private String name;

    private String fatherName;
    private LocalDate dob;
    private String address;
    private String panNumber;
    private String bankAccount;
    private String ifsc;

    private KycStatus status;
}
