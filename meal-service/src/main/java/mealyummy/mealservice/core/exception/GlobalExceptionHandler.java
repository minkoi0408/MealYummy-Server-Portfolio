package mealyummy.mealservice.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import mealyummy.mealservice.core.base.BaseApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {
//  @ExceptionHandler(BadCredentialsException.class)
//  public ResponseEntity<BaseApiResponse> handleBadCredentials(HttpServletRequest request) {
//    return ResponseEntity
//            .status(HttpStatus.UNAUTHORIZED)
//            .body(
//                    BaseApiResponse.error(HttpStatus.UNAUTHORIZED.value(), request.getRequestURI(), "Invalid Email or Password", null)
//            );
//  }


  //  Hàm response lỗi bussiness.
  @ExceptionHandler(AppException.class)
  public ResponseEntity<BaseApiResponse> handleAppException(AppException exception, HttpServletRequest request) {
    ErrorCode errorCode = exception.getErrorCode();

    int status = errorCode.getStatusCode().value();
    String msg = errorCode.getMessage();

    if (exception.getArgs() != null && exception.getArgs().length > 0) {
      msg = String.format(errorCode.getMessage(), exception.getArgs());
    }
    return ResponseEntity
            .status(errorCode.getStatusCode())
            .body(
                    BaseApiResponse.error(msg, null)
            );
  }
  //  Hàm response lỗi bad request.
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseApiResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {

    Map<String, String> errorMap = new HashMap<>();

    exception.getBindingResult().getAllErrors()
            .forEach(error -> {
              String fieldName = ((FieldError) error).getField();
              String enumKey = error.getDefaultMessage();

              String errorMessage;
              try {
                ErrorCode errorCode = ErrorCode.valueOf(enumKey);
                errorMessage = errorCode.getMessage();
              } catch (IllegalArgumentException e) {
                errorMessage = enumKey;
              }

              errorMap.put(fieldName, errorMessage);
            });

    String msg = "Dữ liệu đầu vào không hợp lệ";

    return ResponseEntity.badRequest()
            .body(
                    BaseApiResponse.error(msg, errorMap)
            );
  }
    // Hàm bắt lỗi JSON không hợp lệ
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<BaseApiResponse<Object>> handleJsonError(HttpServletRequest request) {
        String msg = "Định dạng JSON không hợp lệ.";
        return ResponseEntity.badRequest()
                .body(BaseApiResponse.error(msg, null));
    }
}
