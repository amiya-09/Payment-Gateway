package com.springboot.razorpay.merchant.controller;

import com.springboot.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.springboot.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.springboot.razorpay.merchant.dto.response.ApiKeyResponse;
import com.springboot.razorpay.merchant.security.MerchantContext;
import com.springboot.razorpay.merchant.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/merchants/api-keys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantContext merchantContext;

    @PostMapping("/create")
    public ResponseEntity<ApiKeyCreateResponse> create(@Valid @RequestBody CreateApiKeyRequest request){
        log.info("API KEY CREATE CONTROLLER HIT");
        ApiKeyCreateResponse response = apiKeyService.createApiKey(merchantContext.getMerchantId(), request);
        log.info("API KEY CREATE RESPONSE: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getAllApiKeys(){
        return ResponseEntity.ok(apiKeyService.getAllApiKeys(merchantContext.getMerchantId()));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable UUID keyId) {
        apiKeyService.revoke(merchantContext.getMerchantId(), keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<ApiKeyCreateResponse> rotateApiKey(@PathVariable UUID keyId) {
        return ResponseEntity.ok(apiKeyService.rotate(merchantContext.getMerchantId(), keyId));
    }
}
