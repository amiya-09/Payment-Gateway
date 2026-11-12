package com.springboot.razorpay.operations.webhook;


import com.springboot.razorpay.common.enums.WebhookEventStatus;
import com.springboot.razorpay.operations.entity.WebhookEvent;
import com.springboot.razorpay.operations.repository.WebhookEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookRetryQueue retryQueue;
    private final WebhookEventRepository  webhookEventRepository;
    private final WebhookDeliverExecutor webhookDeliverExecutor;

    private ExecutorService virtualThreadExecutor;

    @PostConstruct
    void init() {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }
    @PreDestroy
    void shutdown() {
        virtualThreadExecutor.shutdown();
    }

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int pollBatchSize = 100;

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver() {
        Set<UUID> due = retryQueue.pollDue(pollBatchSize);
        if (due.isEmpty()) {
            log.debug("No due webhook events to deliver.");
            return;
        }
        for(UUID webhookEventId : due) {
            virtualThreadExecutor.submit(() -> webhookDeliverExecutor.deliver(webhookEventId));
            // executor.deliver(webhookEventId)
        }
    }

    // Its responsibility is to find all the item which are in PENDING state and are not yet present in the Redis queue
    // and put them in the redis queue
    @Scheduled(fixedDelay = 10000)
    public void reconcileFromDatabase(){
        LocalDateTime now = LocalDateTime.now();
        List<WebhookEvent> due = webhookEventRepository.findByStatusAndNextRetryAtBefore(WebhookEventStatus.PENDING, now);
        // This is going to find all the events which are due and still inside the PENDING state
        // Find the failed event
        for(WebhookEvent webhookEvent : due){
            retryQueue.enqueueIfAbsent(webhookEvent.getId(), webhookEvent.getNextRetryAt());
        }
    }

}
