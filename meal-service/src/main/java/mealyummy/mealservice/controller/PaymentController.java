package mealyummy.mealservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.payment.MomoService;
import mealyummy.mealservice.service.payment.VnpayService;
import mealyummy.mealservice.service.subscription.PaymentService;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateRequest;
import mealyummy.mealservice.service.subscription.dto.PaymentCreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import mealyummy.mealservice.model.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VnpayService vnpayService;
    private final MomoService momoService;
    private final UserRepository userRepository;

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

    // --- VNPay Webhooks ---

    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        boolean isValid = vnpayService.verifyIpn(params);
        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean isSuccess = isValid && "00".equals(responseCode);

        if (isValid) {
            try {
                paymentService.processPaymentWebhook(orderId, isSuccess);
            } catch (Exception e) {
                System.err.println("❌ [VNPAY_RETURN] Lỗi khi xử lý webhook VNPay: " + e.getMessage());
                e.printStackTrace();
            }
        }

        String redirectUrl = clientUrl + "/membership?payment=" + (isSuccess ? "success" : "failed");
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<String> vnpayIpn(@RequestParam Map<String, String> params) {
        boolean isValid = vnpayService.verifyIpn(params);
        if (!isValid) {
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}");
        }

        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean isSuccess = "00".equals(responseCode);

        try {
            paymentService.processPaymentWebhook(orderId, isSuccess);
            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        } catch (Exception e) {
            return ResponseEntity.ok("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
        }
    }

    // --- MoMo Webhooks ---

    @GetMapping("/momo-return")
    public ResponseEntity<Void> momoReturn(@RequestParam Map<String, String> params) {
        String resultCode = params.get("resultCode");
        String orderId = params.get("orderId");
        boolean isSuccess = "0".equals(resultCode);

        try {
            paymentService.processPaymentWebhook(orderId, isSuccess);
        } catch (Exception e) {
            System.err.println("❌ [MOMO_RETURN] Lỗi khi xử lý webhook MoMo: " + e.getMessage());
            e.printStackTrace();
        }

        String redirectUrl = clientUrl + "/membership?payment=" + (isSuccess ? "success" : "failed");
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/momo-ipn")
    public ResponseEntity<String> momoIpn(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("orderId");
        String amount = payload.get("amount");
        String resultCode = payload.get("resultCode");
        String message = payload.get("message");
        String responseTime = payload.get("responseTime");
        String extraData = payload.get("extraData");
        String signature = payload.get("signature");

        boolean isValid = momoService.verifyIpn(orderId, amount, resultCode, message, responseTime, extraData, signature);
        if (!isValid) {
            return ResponseEntity.badRequest().build();
        }

        boolean isSuccess = "0".equals(resultCode);
        paymentService.processPaymentWebhook(orderId, isSuccess);

        // MoMo expects a 204 No Content for successful IPN acknowledgement usually, or empty 200 OK.
        return ResponseEntity.ok().build();
    }
}
