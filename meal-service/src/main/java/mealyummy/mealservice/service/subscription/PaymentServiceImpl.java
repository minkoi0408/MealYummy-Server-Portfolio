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
        System.out.println("✅ [WEBHOOK] Xử lý transactionId: " + transactionId + ", success: " + isSuccess);
        
        PaymentHistory payment = paymentHistoryRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy Payment với transactionId: " + transactionId));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            System.out.println("⚡ [WEBHOOK] Payment đã xử lý rồi (idempotent), bỏ qua.");
            return; // Idempotent check
        }

        if (isSuccess) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            System.out.println("🎉 [WEBHOOK] Thanh toán thành công, nâng cấp userId: " + payment.getUserId());
            // Upgrade subscription
            subscriptionService.processSuccessfulPayment(payment.getUserId(), payment.getBundleId(), payment.getDurationCode());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        paymentHistoryRepository.save(payment);
    }

    @Override
    public org.springframework.data.domain.Page<PaymentHistory> getAllPaymentHistories(org.springframework.data.domain.Pageable pageable) {
        return paymentHistoryRepository.findAll(pageable);
    }

    @Override
    public PaymentHistory getPaymentHistoryById(String id) {
        return paymentHistoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));
    }
}

