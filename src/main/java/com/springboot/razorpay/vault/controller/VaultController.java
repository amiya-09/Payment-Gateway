package com.springboot.razorpay.vault.controller;

import com.springboot.razorpay.vault.dto.request.TokenizeRequest;
import com.springboot.razorpay.vault.dto.response.TokenizeResponse;
import com.springboot.razorpay.vault.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vault")
public class VaultController {

    private final VaultService vaultService;
    UUID merchantId = UUID.fromString("1f6caca9-cab9-40cd-a491-a714146e5a55"); // TODO: replace it with MerchantContext


    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.tokenize(request, merchantId));
    }
}
