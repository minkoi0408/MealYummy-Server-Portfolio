package mealyummy.mealservice.controller.dashboard.subscription_management;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.service.subscription.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-histories")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentService paymentService;

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<PaymentHistory>>> getAllPaymentHistories(Pageable pageable) {
        Page<PaymentHistory> response = paymentService.getAllPaymentHistories(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách lịch sử thanh toán thành công", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<PaymentHistory>> getPaymentHistory(@PathVariable String id) {
        PaymentHistory response = paymentService.getPaymentHistoryById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin lịch sử thanh toán", response));
    }
}
