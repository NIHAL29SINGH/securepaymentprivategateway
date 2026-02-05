package com.gateway.paymentgateway.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleOAuthService {

    @Value("${google.client.id}")
    private String clientId;

    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            JacksonFactory.getDefaultInstance()
                    )
                            .setAudience(List.of(clientId))
                            .build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null)
                throw new RuntimeException("Invalid Google Token");

            return token.getPayload();

        } catch (Exception e) {
            throw new RuntimeException("Google verification failed", e);
        }
    }
}
