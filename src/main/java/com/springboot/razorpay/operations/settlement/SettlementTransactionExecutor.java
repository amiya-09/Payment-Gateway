package com.springboot.razorpay.operations.settlement;

import com.springboot.razorpay.common.dto.SettlementBankDetails;
import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.common.enums.EventAggregateType;
import com.springboot.razorpay.common.enums.SettlementStatus;
import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.merchant.api.MerchantLookupService;
import com.springboot.razorpay.operations.entity.Settlement;
import com.springboot.razorpay.operations.entity.SettlementPayment;
import com.springboot.razorpay.operations.entity.SettlementPaymentId;
import com.springboot.razorpay.operations.repository.SettlementPaymentRepository;
import com.springboot.razorpay.operations.repository.SettlementRepository;
import com.springboot.razorpay.operations.settlement.dto.BankTransferResult;
import com.springboot.razorpay.payment.api.PaymentLookupService;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTransactionExecutor {

    private static final double FEE_RATE = 0.02;
    private static final double GST_RATE = 0.18;

    private final PaymentLookupService paymentLookupService;
    private final SettlementRepository settlementRepository;
    private final SettlementPaymentRepository settlementPaymentRepository;
    private final MerchantLookupService merchantLookupService;
    private final BankTransferProcessor bankTransferProcessor;
    // TODO : PUBLISHER INSIDE IT'S OWN DB
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void processForMerchant(UUID merchantId, LocalDate settlementDate) {

        log.info("Processing settlement for merchantId: {}, settlementDate: {}", merchantId, settlementDate);
        // Implement the logic to process settlement for the given merchant and date.
        List<Payment> unsettledPayments = paymentLookupService.findUnsettledCapturedPayments(merchantId);
        if (unsettledPayments.isEmpty()) return;

        log.info("Processing {} unsettled payments for merchantId: {}, settlementDate: {}", unsettledPayments.size(), merchantId, settlementDate);

        Money gross = unsettledPayments.stream()
                .map(Payment::getAmount)
                .reduce(Money::add)
                .orElseThrow();

        int fee = Math.toIntExact(Math.round(gross.getAmountUnits() * FEE_RATE));
        int gst = Math.toIntExact(Math.round(fee * GST_RATE));
        Money feeAmount = Money.of(fee, gross.getCurrency());
        Money gstAmount = Money.of(gst, gross.getCurrency());
        Money netAmount = gross.subtract(feeAmount).subtract(gstAmount);

        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .grossAmount(gross)
                .feeAmount(feeAmount)
                .gstAmount(gstAmount)
                .netAmount(netAmount)
                .status(SettlementStatus.INITIATED)
                .build();

        settlementRepository.save(settlement);

        try {// this list contains all the payments that are part of the settlement
            List<SettlementPayment> links = new ArrayList<>();
            for (Payment payment : unsettledPayments) {
                links.add(SettlementPayment.builder()
                        .id(new SettlementPaymentId(settlement.getId(), payment.getId()))
                        .settlement(settlement)
                        .build());
            }
            settlementPaymentRepository.saveAll(links);

            SettlementBankDetails settlementBankDetails = merchantLookupService.getSettlementBankDetails(merchantId);

            // call the bank transfer service to transfer net amount to merchant settlement bank details
            BankTransferResult bankTransferResult = bankTransferProcessor.initiate(settlement.getId(), merchantId,
                    netAmount, settlementBankDetails.accountNumber(), settlementBankDetails.ifscCode());

            // mark the payments as settled so the next settlement run does not pick them up again
            LocalDateTime settledAt = LocalDateTime.now();
            unsettledPayments.forEach(payment -> payment.setSettledAt(settledAt));

            settlement.setStatus(SettlementStatus.TRANSFER_PENDING);
            settlement.setBankReference(bankTransferResult.registrationRef());
            settlementRepository.save(settlement);
        } catch (Exception e) {
            log.error("Error while processing settlement for merchantId: {}, settlementDate: {}", merchantId, settlementDate, e);
            settlement.setStatus(SettlementStatus.FAILED);
            settlementRepository.save(settlement);
        }

    }

    @Transactional
    public void resolveTransfer(UUID settlementId, String errorCode, String errorDescription) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", settlementId));

        if(settlement.getStatus() != SettlementStatus.TRANSFER_PENDING) {
            log.info("Settlement resolved, skipping for id: {}", settlement.getId());
            return;
        }

        if(errorCode == null){
            settlement.setStatus(SettlementStatus.PROCESSED);
            settlement.setProcessedAt(LocalDateTime.now());
            settlementRepository.save(settlement);
            log.info("Settlement processed successfully, settlement id: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_PROCESSED", Map.of(
                            "settlementId", settlementId.toString(),
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        } else {
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailureReason(errorCode+" : "+errorDescription);
            settlementRepository.save(settlement);
            log.warn("Settlement failed, settlement id: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_FAILED", Map.of(
                            "settlementId", settlementId.toString(),
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        }

    }

}
