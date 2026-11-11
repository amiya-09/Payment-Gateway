package com.springboot.razorpay.merchant.mapper;

import com.springboot.razorpay.merchant.dto.response.WebhookConfigResponse;
import com.springboot.razorpay.merchant.entity.MerchantWebhookConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WebhookConfigMapper {

    @Mapping(target = "webhookSecret", source = "rawSecret")
    WebhookConfigResponse toResponse(MerchantWebhookConfig config, String rawSecret);
}
