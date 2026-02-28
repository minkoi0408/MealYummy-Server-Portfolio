package mealyummy.mealservice.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    CATEGORY_NOT_FOUND("Không tìm thấy danh mục hoặc danh mục đã bị hạn chế", HttpStatus.CONFLICT),
    CATEGORY_INVALID_NAME("Tên danh mục không hợp lệ hoặc đã bị trùng", HttpStatus.UNPROCESSABLE_CONTENT),
    CATEGORY_PARENT_NOT_FOUND("Danh mục cha không tồn tại hoặc đã bị hạn chế", HttpStatus.CONFLICT)
    ,
    ;
    ErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String message;
    private final HttpStatus statusCode;
}