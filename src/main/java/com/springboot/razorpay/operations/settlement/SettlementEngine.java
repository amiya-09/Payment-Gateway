package com.springboot.razorpay.operations.settlement;

import com.springboot.razorpay.merchant.api.MerchantLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementEngine {

    private final MerchantLookupService merchantLookupService;
    private final SettlementTransactionExecutor settlementTransactionExecutor;

    @Scheduled(cron = "0 0 23 * * *") // this means that at 23 hours at 0 minutes, 0 seconds, do this every day of the week of the month
    public void runScheduled() {
        // this needs to run every day at a particular moment in time
        log.info("Running Settlement Engine");
        run();
    }

    public void run() {
        List<UUID> merchantIds = merchantLookupService.listActiveMerchantIds();
        log.info("Processing the settlement engine for {} merchants", merchantIds.size());

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> futures = new ArrayList<>(); // future is basically a promise that the task will be completed in the future, and we can get the result of the task when it is completed
            for (UUID merchantId : merchantIds) {
                futures.add(executorService.submit(() -> {
                    // call the settlement executor to process the settlement for this merchant
                    settlementTransactionExecutor.processForMerchant(merchantId, LocalDate.now());
                }));
            }

            for(Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error while processing settlement for a merchant", e);
                    throw new RuntimeException(e);
                }
            }
        }
        log.info("Completed processing the settlement engine for {} merchants", merchantIds.size());
    }
}
