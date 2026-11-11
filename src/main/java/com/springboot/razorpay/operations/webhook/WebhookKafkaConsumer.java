package com.springboot.razorpay.operations.webhook;

import com.springboot.razorpay.common.dto.WebhookTarget;
import com.springboot.razorpay.common.enums.WebhookEventStatus;
import com.springboot.razorpay.common.util.SignerUtil;
import com.springboot.razorpay.merchant.api.MerchantWebhookApi;
import com.springboot.razorpay.operations.entity.WebhookEvent;
import com.springboot.razorpay.operations.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookKafkaConsumer {

    private final MerchantWebhookApi merchantWebhookApi;
    private final ObjectMapper objectMapper;
    private final SignerUtil signerUtil;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue retryQueue;

    @KafkaListener(topics = {
            "${app.kafka.topics.payment:payment.events}",
            "${app.kafka.topics.order:order.events}",
            "${app.kafka.topics.refund:refund.events}",
            "${app.kafka.topics.settlement:settlement.events}",
    })
    public void onWebhookEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {

        try {
            Map<String, Object> envelope = record.value();
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            String eventType = (String) envelope.get("eventType");

            Object merchantIdRaw = data.get("merchantId");
            if (merchantIdRaw == null) {
                log.warn("Received webhook event without merchantId, skipping processing. EventType: {}, Data: {}", eventType, data);
                ack.acknowledge();
                return;
            }
            UUID merchantId = UUID.fromString(merchantIdRaw.toString());

            List<WebhookTarget> targets = merchantWebhookApi.getActiveConfigForEvent(merchantId, eventType);
            if (targets.isEmpty()) {
                log.debug("No active webhook targets found for merchantId: {}, eventType: {}", merchantId, eventType);
                ack.acknowledge();
                return;
            }
            // Now we have a list of webhook targets, and we want to call the API that we have found in the target URL,
            // but we don't call the API directly from here, we will send the event to a queue, and then we will have a
            // separate service that will read from the queue and call the API.
            // This is because we don't want to block the Kafka consumer thread, and we also want to be able to retry the
            // webhook calls in case of failures. So we will send the event to a queue, and then we will have a separate
            // service that will read from the queue and call the API. This way we can also have a retry mechanism in place for failed webhook calls.
            Map<String, Object> signatureData = Map.of("event", eventType, "payload", data);
            String signatureJson = objectMapper.writeValueAsString(signatureData);

            for (WebhookTarget target : targets) {
                String signature = signerUtil.sign(signatureJson, target.webhookSecret());

                WebhookEvent webhookEvent = WebhookEvent.builder()
                        .merchantId(merchantId)
                        .eventType(eventType)
                        .payload(data)
                        .targetUrl(target.targetUrl())
                        .signature(signature)
                        .status(WebhookEventStatus.PENDING)
                        .nextRetryAt(LocalDateTime.now())
                        .build();

                webhookEvent = webhookEventRepository.save(webhookEvent);

                retryQueue.enqueue(webhookEvent.getId(), webhookEvent.getNextRetryAt());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing webhook event from Kafka record, offset: {}", record.offset(), e);
            // Do not acknowledge the message, so it can be retried later.
            // TODO : CHECK EXCEPTION FOR ACKNOWLEDGING
        }

    }

}
