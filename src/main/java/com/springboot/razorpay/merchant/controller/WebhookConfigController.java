package com.springboot.razorpay.merchant.controller;

import com.springboot.razorpay.merchant.dto.request.UpdateWebhookConfigRequest;
import com.springboot.razorpay.merchant.dto.response.WebhookConfigResponse;
import com.springboot.razorpay.merchant.security.MerchantContext;
import com.springboot.razorpay.merchant.service.WebhookConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/merchants/webhooks")
@RequiredArgsConstructor
public class WebhookConfigController {
    private final WebhookConfigService webhookConfigService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<WebhookConfigResponse> create(
            @Valid @RequestBody UpdateWebhookConfigRequest request) {
        return ResponseEntity.ok(
                webhookConfigService.create(merchantContext.getMerchantId(), request));
    }

    @GetMapping
    public ResponseEntity<List<WebhookConfigResponse>> list() {
        return ResponseEntity.ok(
                webhookConfigService.findAllByMerchantId(merchantContext.getMerchantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                webhookConfigService.findById(merchantContext.getMerchantId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookConfigRequest request) {
        return ResponseEntity.ok(
                webhookConfigService.update(merchantContext.getMerchantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookConfigService.delete(merchantContext.getMerchantId(), id);
        return ResponseEntity.noContent().build();
    }
}
