package mealyummy.mealservice.service.subscription;

import jakarta.servlet.http.HttpServletRequest;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateRequest;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateResponse;
import mealyummy.mealservice.service.subscription.dto.PaymentHistoryResponseDTO;

public interface PaymentService {
    PaymentHistory recordPayment(PaymentHistory paymentHistory);
    PaymentCreateResponse createPaymentUrl(PaymentCreateRequest request, HttpServletRequest httpRequest);
    void processPaymentWebhook(String transactionId, boolean isSuccess);
    org.springframework.data.domain.Page<PaymentHistoryResponseDTO> getAllPaymentHistories(org.springframework.data.domain.Pageable pageable);
    PaymentHistory getPaymentHistoryById(String id);
}
