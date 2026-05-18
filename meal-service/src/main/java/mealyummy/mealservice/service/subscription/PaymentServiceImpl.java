package mealyummy.mealservice.service.subscription;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.model.enums.PaymentMethod;
import mealyummy.mealservice.model.enums.PaymentStatus;
import mealyummy.mealservice.model.pojo.BundleDuration;
import mealyummy.mealservice.model.repository.subscription.PaymentHistoryRepository;
import mealyummy.mealservice.service.payment.MomoService;
import mealyummy.mealservice.service.payment.VnpayService;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateRequest;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BundleService bundleService;
    private final SubscriptionService subscriptionService;
    private final VnpayService vnpayService;
    private final MomoService momoService;

    @Override
    public PaymentHistory recordPayment(PaymentHistory paymentHistory) {
        return paymentHistoryRepository.save(paymentHistory);
    }

    @Override
    public PaymentCreateResponse createPaymentUrl(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        // Validate bundle
        Bundle bundle = bundleService.getBundleById(request.getBundleId());
        BundleDuration duration = bundle.getDurations().stream()
                .filter(d -> d.getDurationCode().equals(request.getDurationCode()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DURATION_INVALID));

        // Create transaction ID
        String transactionId = UUID.randomUUID().toString();

        // Save pending payment history
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .userId(request.getUserId())
                .bundleId(bundle.getId())
                .durationCode(duration.getDurationCode())
                .amount(duration.getPrice())
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(transactionId)
                .build();
        paymentHistoryRepository.save(paymentHistory);

        // Generate URL
        String paymentUrl = "";
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentUrl = vnpayService.createPaymentUrl(transactionId, duration.getPrice(), httpRequest);
        } else if (request.getPaymentMethod() == PaymentMethod.MOMO) {
            paymentUrl = momoService.createPaymentUrl(transactionId, duration.getPrice());
        }

        return PaymentCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .orderId(transactionId)
                .build();
    }

    @Override
    public void processPaymentWebhook(String transactionId, boolean isSuccess) {
        PaymentHistory payment = paymentHistoryRepository.findAll().stream()
                .filter(p -> p.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction ID not found"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // Idempotent check
        }

        if (isSuccess) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            // Upgrade subscription
            subscriptionService.processSuccessfulPayment(payment.getUserId(), payment.getBundleId(), payment.getDurationCode());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        paymentHistoryRepository.save(payment);
    }
}

