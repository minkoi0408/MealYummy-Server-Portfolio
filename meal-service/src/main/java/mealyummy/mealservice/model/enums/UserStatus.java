package mealyummy.mealservice.model.enums;

public enum UserStatus {
    PENDING,    // Chờ xác thực OTP
    ACTIVE,     // Đã xác thực, hoạt động bình thường
    BANNED      // Bị khóa
}
