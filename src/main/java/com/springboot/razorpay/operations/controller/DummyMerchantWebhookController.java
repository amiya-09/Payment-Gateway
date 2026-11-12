package com.springboot.razorpay.operations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class DummyMerchantWebhookController {

    @PostMapping("/success")
    public ResponseEntity<Void> handleWebhookSuccess(@RequestBody Map<String, Object> requestBody) {
        // Process the webhook data
        return ResponseEntity.noContent().build();
    }
}
