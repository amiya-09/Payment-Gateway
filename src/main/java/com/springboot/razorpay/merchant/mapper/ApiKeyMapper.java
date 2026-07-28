package com.springboot.razorpay.merchant.mapper;

import com.springboot.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.springboot.razorpay.merchant.dto.response.ApiKeyResponse;
import com.springboot.razorpay.merchant.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiKeyMapper {
    ApiKeyCreateResponse toApiKeyCreateResponse(ApiKey apiKey);

    List<ApiKeyResponse> toApiKeyResponses(List<ApiKey> apiKeyList);
}
