package com.springboot.razorpay.merchant.api;

import com.springboot.razorpay.common.dto.SettlementBankDetails;
import com.springboot.razorpay.common.dto.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantLookupService {

    List<WebhookTarget> getActiveConfigForEvent(UUID merchantId, String eventType);

    List<UUID> listActiveMerchantIds();

    SettlementBankDetails getSettlementBankDetails(UUID merchantId);

}
