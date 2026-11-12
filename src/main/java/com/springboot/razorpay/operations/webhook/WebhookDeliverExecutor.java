package com.springboot.razorpay.operations.webhook;

import com.springboot.razorpay.common.enums.WebhookEventStatus;
import com.springboot.razorpay.operations.entity.WebhookEvent;
import com.springboot.razorpay.operations.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliverExecutor {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue webhookRetryQueue;
    private final RestClient restClient;

    private static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(8),
            Duration.ofHours(24)
    );

    private final int MAX_ATTEMPTS = 7;

    @Value("${app.webhook.delivery.signature-header:X-Razorpay-Signature}")
    private String signatureHeader;

    public void deliver(UUID webhookEventId) {
        Optional<WebhookEvent> webhookEvent = webhookEventRepository.findById(webhookEventId);

        if (webhookEvent.isEmpty()) {
            log.warn("Webhook event with id {} not found", webhookEventId);
            return;
        }

        WebhookEvent event = webhookEvent.get();

        if(event.getStatus() == WebhookEventStatus.DELIVERED ||  event.getStatus() == WebhookEventStatus.DEAD) {
            log.warn("Cannot deliver webhook event with id {}", webhookEventId);
            return;
        }

        event.setAttempts(event.getAttempts() + 1);
        event.setLastAttemptAt(LocalDateTime.now());

        try {
            var response = restClient.post()
                    .uri(event.getTargetUrl())
                    .header(signatureHeader, event.getSignature())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "event", event.getEventType(), "payload", event.getPayload()
                    )).retrieve()
                    .toBodilessEntity();

            int statusCode = response.getStatusCode().value();
            event.setLastResponseCode(statusCode);

            if(response.getStatusCode().is2xxSuccessful()) {
                event.setStatus(WebhookEventStatus.DELIVERED);
                event.setDeliveredAt(LocalDateTime.now());
                webhookEventRepository.save(event);
                return;
            }

            handleAttemptFailed(event, "HTTP"+statusCode);

        } catch (RestClientException e) {
            log.error("Error delivering webhook event with id {}: {}", webhookEventId, e.getMessage());
            event.setLastResponseBody(e.getMessage());
            handleAttemptFailed(event, e.getMessage());
        }
    }

    private void handleAttemptFailed(WebhookEvent event, String error) {
        event.setLastResponseBody(error);
        if(event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(WebhookEventStatus.DEAD);
            // DLQ Recordings
            return;
        }

        Duration backoff = BACKOFF.get(event.getAttempts()-1);
        LocalDateTime nextRetryAt = LocalDateTime.now().plus(backoff);
        event.setStatus(WebhookEventStatus.FAILED);
        event.setNextRetryAt(nextRetryAt);
        webhookEventRepository.save(event);
        webhookRetryQueue.enqueue(event.getId(), nextRetryAt);
    }

}
