package com.gateway.paymentgateway.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class KycRequest {
    private String fatherName;
    private LocalDate dob;
    private String address;
    private String panNumber;
    private String bankAccount;
    private String ifsc;
}
