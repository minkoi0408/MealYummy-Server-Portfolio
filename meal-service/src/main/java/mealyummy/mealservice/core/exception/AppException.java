package mealyummy.mealservice.core.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
    private Object[] args;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }
}