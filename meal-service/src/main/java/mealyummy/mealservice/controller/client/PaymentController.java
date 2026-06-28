package mealyummy.mealservice.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.subscription.PaymentService;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateRequest;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import mealyummy.mealservice.model.repository.UserRepository;
import vn.payos.PayOS;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final PayOS payOS;

    @Value("${app.client-url}")
    private String clientUrl;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseApiResponse<PaymentCreateResponse>> createPayment(
            @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest,
            org.springframework.security.core.Authentication authentication) {
        
        // Tự lấy userId từ JWT (an toàn, không tin vào client)
        String username = authentication.getName();
        mealyummy.mealservice.model.entity.auth.User currentUser = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new mealyummy.mealservice.core.exception.AppException(mealyummy.mealservice.core.exception.ErrorCode.USER_NOT_FOUND));
        request.setUserId(currentUser.getId());
        System.out.println("✅ [CREATE_PAYMENT] userId: " + currentUser.getId() + " | bundle: " + request.getBundleId());
        
        PaymentCreateResponse response = paymentService.createPaymentUrl(request, httpRequest);
        return ResponseEntity.ok(BaseApiResponse.ok("Tạo URL thanh toán thành công", response));
    }

    // --- PayOS Webhooks & Redirects ---

    @GetMapping("/payos-return")
    public ResponseEntity<Void> payosReturn(@RequestParam Map<String, String> params) {
        String status = params.get("status");
        String code = params.get("code");
        String orderCode = params.get("orderCode");
        boolean isSuccess = "00".equals(code) || "PAID".equals(status);

        if (orderCode != null) {
            try {
                paymentService.processPaymentWebhook(orderCode, isSuccess);
            } catch (Exception e) {
                System.err.println("❌ [PAYOS_RETURN] Lỗi khi xử lý return: " + e.getMessage());
                e.printStackTrace();
            }
        }

        String redirectUrl = clientUrl + "/membership?payment=" + (isSuccess ? "success" : "failed");
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/payos-cancel")
    public ResponseEntity<Void> payosCancel(@RequestParam Map<String, String> params) {
        String orderCode = params.get("orderCode");
        if (orderCode != null) {
            try {
                paymentService.processPaymentWebhook(orderCode, false);
            } catch (Exception e) {
                System.err.println("❌ [PAYOS_CANCEL] Lỗi khi xử lý cancel: " + e.getMessage());
            }
        }
        String redirectUrl = clientUrl + "/membership?payment=failed";
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/payos-webhook")
    public ResponseEntity<Void> payosWebhook(@RequestBody Webhook webhookBody) {
        try {
            WebhookData data = payOS.webhooks().verify(webhookBody);
            String transactionId = String.valueOf(data.getOrderCode());
            boolean isSuccess = "00".equals(data.getCode());
            
            paymentService.processPaymentWebhook(transactionId, isSuccess);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ [PAYOS_WEBHOOK] Lỗi xác thực hoặc xử lý webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
