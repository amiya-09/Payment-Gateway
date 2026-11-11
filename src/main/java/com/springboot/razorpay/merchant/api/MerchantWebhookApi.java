package com.springboot.razorpay.merchant.api;

import com.springboot.razorpay.common.dto.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantWebhookApi {

    List<WebhookTarget> getActiveConfigForEvent(UUID merchantId, String eventType);

}
