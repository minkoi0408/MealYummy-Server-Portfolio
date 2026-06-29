package mealyummy.mealservice.service.subscription;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.config.PayosConfig;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.model.enums.PaymentMethod;
import mealyummy.mealservice.model.enums.PaymentStatus;
import mealyummy.mealservice.model.pojo.BundleDuration;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.model.repository.subscription.BundleRepository;
import mealyummy.mealservice.model.repository.subscription.PaymentHistoryRepository;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateRequest;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateResponse;
import mealyummy.mealservice.service.subscription.dto.PaymentHistoryResponseDTO;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BundleService bundleService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final BundleRepository bundleRepository;
    private final PayOS payOS;
    private final PayosConfig payosConfig;

    @Override
    public PaymentHistory recordPayment(PaymentHistory paymentHistory) {
        return paymentHistoryRepository.save(paymentHistory);
    }

    @Override
    public PaymentCreateResponse createPaymentUrl(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        Bundle bundle = bundleService.getBundleById(request.getBundleId());
        BundleDuration duration = bundle.getDurations().stream()
                .filter(d -> d.getDurationCode().equals(request.getDurationCode()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DURATION_INVALID));

        long orderCode = System.currentTimeMillis() / 1000 * 100000 + (new java.util.Random().nextInt(90000) + 10000);
        String transactionId = String.valueOf(orderCode);

        PaymentHistory paymentHistory = PaymentHistory.builder()
                .userId(request.getUserId())
                .bundleId(bundle.getId())
                .durationCode(duration.getDurationCode())
                .amount(duration.getPrice())
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(transactionId)
                .build();
        paymentHistoryRepository.save(paymentHistory);

        String paymentUrl = "";
        if (request.getPaymentMethod() == PaymentMethod.PAYOS) {
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) duration.getPrice())
                    .description("MealYummy VIP")
                    .returnUrl(payosConfig.getReturnUrl())
                    .cancelUrl(payosConfig.getCancelUrl())
                    .build();
            try {
                CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentRequest);
                paymentUrl = response.getCheckoutUrl();
            } catch (Exception e) {
                e.printStackTrace();
                throw new AppException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND);
            }
        }

        return PaymentCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .orderId(transactionId)
                .build();
    }

    @Override
    public void processPaymentWebhook(String transactionId, boolean isSuccess) {
        PaymentHistory payment = paymentHistoryRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        if (isSuccess) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            subscriptionService.processSuccessfulPayment(payment.getUserId(), payment.getBundleId(), payment.getDurationCode());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        paymentHistoryRepository.save(payment);
    }

    @Override
    public org.springframework.data.domain.Page<PaymentHistoryResponseDTO> getAllPaymentHistories(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<PaymentHistory> page = paymentHistoryRepository.findAll(pageable);
        return page.map(p -> {
            String uname = p.getUserId();
            String bname = p.getBundleId();
            
            try {
                if (p.getUserId() != null) {
                    User u = userRepository.findById(p.getUserId()).orElse(null);
                    if (u != null && u.getUsername() != null) uname = u.getUsername();
                }
            } catch (Exception e) {
                log.error("Error finding user for ID: " + p.getUserId(), e);
            }

            try {
                if (p.getBundleId() != null) {
                    Bundle b = bundleRepository.findById(p.getBundleId()).orElse(null);
                    if (b != null && b.getName() != null) bname = b.getName();
                }
            } catch (Exception e) {
                log.error("Error finding bundle for ID: " + p.getBundleId(), e);
            }
            
            return PaymentHistoryResponseDTO.builder()
                    .id(p.getId())
                    .txId(p.getTransactionId()) // Using transactionId as txId
                    .userId(p.getUserId())
                    .bundleId(p.getBundleId())
                    .durationCode(p.getDurationCode())
                    .amount(p.getAmount())
                    .paymentStatus(p.getPaymentStatus())
                    .transactionId(p.getTransactionId())
                    .paidAt(p.getPaidAt())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .username(uname)
                    .bundleName(bname)
                    .build();
        });
    }

    @Override
    public PaymentHistory getPaymentHistoryById(String id) {
        return paymentHistoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));
    }
}
