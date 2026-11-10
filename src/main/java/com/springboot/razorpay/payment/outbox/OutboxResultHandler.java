package com.springboot.razorpay.payment.outbox;

import com.springboot.razorpay.common.enums.OutboxStatus;
import com.springboot.razorpay.payment.entity.OutboxEvent;
import com.springboot.razorpay.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxResultHandler {
    private final OutboxEventRepository outboxEventRepository;
    private final Integer MAX_ATTEMPTS = 3;

    @Transactional
    public void handleEventPublished(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
        log.info("Outbox event published successfully: {}", event.getId());
    }

    @Transactional
    public void handleEventFailed(OutboxEvent event, String errorMessage) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(errorMessage.length() < 1000 ? errorMessage : errorMessage.substring(0, 1000));
        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
        }
        outboxEventRepository.save(event);
        log.error("Outbox event failed: {}", event.getId());
    }
}
