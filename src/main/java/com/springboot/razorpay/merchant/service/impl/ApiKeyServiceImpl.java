package com.springboot.razorpay.merchant.service.impl;

import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.springboot.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.springboot.razorpay.merchant.dto.response.ApiKeyResponse;
import com.springboot.razorpay.merchant.entity.ApiKey;
import com.springboot.razorpay.merchant.entity.Merchant;
import com.springboot.razorpay.merchant.mapper.ApiKeyMapper;
import com.springboot.razorpay.merchant.repository.ApiKeyRepository;
import com.springboot.razorpay.merchant.repository.MerchantRepository;
import com.springboot.razorpay.merchant.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    @Override
    @Transactional
    public ApiKeyCreateResponse createApiKey(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(()->new ResourceNotFoundException("merchant" , merchantId));
        // Here you would implement the logic to create an API key for the merchant.
        // For demonstration purposes, let's assume we generate a random keyId and keySecret.
        String keyId = "rzp_"+request.environment().name().toLowerCase()+"_"+ RandomizerUtil.randomBase64(24);
        String rawSecret = RandomizerUtil.randomBase64(40);
        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(rawSecret) // TODO: encode with BcryptPasswordEncoder
                .environment(request.environment())
                .build();
        apiKey = apiKeyRepository.save(apiKey);
        return new ApiKeyCreateResponse(apiKey.getId(), keyId, rawSecret, request.environment());
    }

    @Override
    public List<ApiKeyResponse> getAllApiKeys(UUID merchantId) {
//        return apiKeyRepository.findByMerchant_Id(merchantId).stream()
//                .map(apiKey -> new ApiKeyResponse(
//                        apiKey.getId(),
//                        apiKey.getKeyId(),
//                        apiKey.getEnvironment(),
//                        apiKey.isEnabled(),
//                        apiKey.getLastUsedAt(),
//                        null
//                ))
//                .toList();
        return apiKeyMapper.toApiKeyResponses(apiKeyRepository.findByMerchant_Id(merchantId));
    }

    @Override
    @Transactional
    public void revoke(UUID merchantId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(key -> key.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new ResourceNotFoundException("apiKey", keyId));
        apiKey.setEnabled(false);
        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public ApiKeyCreateResponse rotate(UUID merchantId, UUID keyId) {
        // Check first if api key is here or not , throw an exception if not found
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(key -> key.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new ResourceNotFoundException("apiKey", keyId));

        if(!apiKey.isEnabled()) throw new RuntimeException("Cannot rotate a disabled apiKey");
        // Now we generate a new secret key and send it over to the merchant
        String newRawSecret = RandomizerUtil.randomBase64(40);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(newRawSecret);
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusHours(24)); // Set grace period for 24 hours
        apiKeyRepository.save(apiKey);
        return new ApiKeyCreateResponse(apiKey.getId(), apiKey.getKeyId(), newRawSecret, apiKey.getEnvironment());
    }
}
