package mealyummy.mealservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.iam.authentication.dto.LoginRequestDTO;
import mealyummy.mealservice.service.iam.notification.NotificationService;
import mealyummy.mealservice.service.iam.notification.dto.SendOtpToEmailRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/otp-registration")
    public ResponseEntity<BaseApiResponse<String>> sendOtpRegistration(
            @Valid @RequestBody SendOtpToEmailRequestDTO request) {

        String message = notificationService.sendOtpRegistration(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, null));
    }

    @PostMapping("/otp-forgot-password")
    public ResponseEntity<BaseApiResponse<String>> sendOtpForgotPassword(
            @Valid @RequestBody SendOtpToEmailRequestDTO request) {

        String message = notificationService.sendOtpForgotPassword(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, null));
    }

    @PostMapping("/otp-login")
    public ResponseEntity<BaseApiResponse<String>> sendOtpLogin(
            @Valid @RequestBody LoginRequestDTO request) {

        String message = notificationService.sendOtpLogin(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, null));
    }
}
