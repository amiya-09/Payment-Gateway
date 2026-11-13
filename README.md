## Homework

### Refund Flow

1. Enums - RefundStatus
2. Entity - Refund
3. Repository - RefundRepository
4. Service - RefundService/RefundServiceImpl
5. State Machine - RefundStateMachine + RefundEvent
6. Async Processor - RefundProcessor (@Scheduled job pulling PENDING/PROCESSING refunds and processing them)
7. Controller - RefundController (post /v1/payments/{paymentId}/refund and GET /v1/payments/refunds/{refundId})

Implement the above flow for refunds. The refund flow should be similar to the payment flow.