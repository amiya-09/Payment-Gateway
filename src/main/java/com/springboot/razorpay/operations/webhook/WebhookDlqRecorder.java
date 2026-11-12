package com.springboot.razorpay.operations.webhook;

import com.springboot.razorpay.common.enums.WebhookEventStatus;
import com.springboot.razorpay.operations.entity.DlqEvent;
import com.springboot.razorpay.operations.entity.WebhookEvent;
import com.springboot.razorpay.operations.repository.DlqEventRepository;
import com.springboot.razorpay.operations.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDlqRecorder {

    private final WebhookEventRepository webhookEventRepository;
    private final DlqEventRepository dlqEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAfterAttemptExhausted (WebhookEvent webhookEvent, String finalError) {
        log.debug("Recording the dlq event with webhookEventId: {}, merchantId: {}",webhookEvent.getId(), webhookEvent.getMerchantId());
        webhookEvent.setStatus(WebhookEventStatus.DEAD);
        webhookEventRepository.save(webhookEvent);

        DlqEvent dlqEvent = DlqEvent.builder()
                .webhookEvent(webhookEvent)
                .merchantId(webhookEvent.getMerchantId())
                .finalError(finalError)
                .payload(webhookEvent.getPayload())
                .build();
        dlqEventRepository.save(dlqEvent);
    }

    public void recordConsumerFailed(ConsumerRecord<String, Map<String, Object>> record, String error) {
        Map<String, Object> envelope = record.value();
        UUID merchantId = null;
        try {
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            Object merchantIdRaw = data != null ? data.get("merchantId") : null;
            if (merchantIdRaw != null) {
                merchantId = UUID.fromString(merchantIdRaw.toString());
            }
        } catch (Exception ignored) {
            log.error("Error while extracting merchantId from webhook event", ignored);
        }

        log.debug("Recording the dlq because consumer failed with merchantId: {}", merchantId);
        DlqEvent dlqEvent = DlqEvent.builder()
                .webhookEvent(null)
                .merchantId(merchantId)
                .finalError(error)
                .payload(envelope != null ? envelope: Map.of())
                .build();

        dlqEventRepository.save(dlqEvent);
    }
}
