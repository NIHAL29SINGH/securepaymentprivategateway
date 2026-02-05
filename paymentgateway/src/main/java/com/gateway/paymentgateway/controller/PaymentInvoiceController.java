package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.request.InvoicePdfRequest;
import com.gateway.paymentgateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/invoice")
@RequiredArgsConstructor
public class PaymentInvoiceController {

    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/pdf")
    public String sendPdfInvoice(@RequestBody InvoicePdfRequest request) {

        paymentService.sendInvoicePdf(
                request.getEmail(),
                request.getPassword(),
                passwordEncoder
        );

        return "Invoice PDF sent to email successfully";
    }
}
