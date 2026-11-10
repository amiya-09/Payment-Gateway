package com.springboot.razorpay.payment.outbox;

import com.springboot.razorpay.common.config.KafkaProperties;
import com.springboot.razorpay.common.enums.OutboxStatus;
import com.springboot.razorpay.payment.entity.OutboxEvent;
import com.springboot.razorpay.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final OutboxResultHandler outboxResultHandler;

    @Scheduled(fixedDelay = 5000)
    public void poll(){
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            try {
                    String topic = kafkaProperties.topicFor(event.getAggregateType());
                    String key = extractMerchantId(event.getPayload());
                    Map<String, Object> envelope = Map.of(
                            "eventType", event.getEventType(),
                            "aggregateType", event.getAggregateType().name(),
                            "aggregateId", event.getAggregateId().toString(),
                            "data", event.getPayload()
                    );
                    kafkaTemplate.send(topic, key, envelope).get(5, TimeUnit.SECONDS);

                    outboxResultHandler.handleEventPublished(event);
                } catch (Exception e) {
                    log.error("outbox event failed, eventId: {}, attempts: {}", event.getId(), event.getAttempts());
                    outboxResultHandler.handleEventFailed(event, e.getMessage());
                }
        }
    }

    private String extractMerchantId(Map<String,Object> payload){
        Object merchantId = payload.get("merchantId");
        if(merchantId == null){
            log.warn("Merchant ID not found in payload: {}", payload);
            return "unknown";
        }
        return merchantId.toString();

    }
}
