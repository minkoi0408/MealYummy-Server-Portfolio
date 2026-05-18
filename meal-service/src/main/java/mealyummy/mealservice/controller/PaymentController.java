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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VnpayService vnpayService;
    private final MomoService momoService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseApiResponse<PaymentCreateResponse>> createPayment(
            @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest) {
        
        PaymentCreateResponse response = paymentService.createPaymentUrl(request, httpRequest);
        return ResponseEntity.ok(BaseApiResponse.ok("Tạo URL thanh toán thành công", response));
    }

    // --- VNPay Webhooks ---

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> params) {
        // Here you would normally redirect the user to a frontend success/failure page
        // e.g., return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://frontend/success")).build();
        
        boolean isValid = vnpayService.verifyIpn(params);
        if (!isValid) {
            return ResponseEntity.badRequest().body("Giao dịch không hợp lệ (Sai chữ ký)");
        }
        
        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            return ResponseEntity.ok("Thanh toán thành công! Bạn có thể quay lại ứng dụng.");
        } else {
            return ResponseEntity.ok("Thanh toán thất bại hoặc đã bị hủy.");
        }
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
    public ResponseEntity<String> momoReturn(@RequestParam Map<String, String> params) {
        String resultCode = params.get("resultCode");
        if ("0".equals(resultCode)) {
            return ResponseEntity.ok("Thanh toán MoMo thành công! Bạn có thể quay lại ứng dụng.");
        } else {
            return ResponseEntity.ok("Thanh toán MoMo thất bại hoặc đã bị hủy.");
        }
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
