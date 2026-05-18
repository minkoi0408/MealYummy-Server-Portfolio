package mealyummy.mealservice.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    CATEGORY_NOT_FOUND("Không tìm thấy danh mục hoặc danh mục đã bị hạn chế", HttpStatus.CONFLICT),
    CATEGORY_INVALID_NAME("Tên danh mục không hợp lệ hoặc đã bị trùng", HttpStatus.UNPROCESSABLE_CONTENT),
    CATEGORY_PARENT_NOT_FOUND("Danh mục cha không tồn tại hoặc đã bị hạn chế", HttpStatus.CONFLICT),


    INGREDIENT_NOT_FOUND("Không tìm thấy nguyên liệu hoặc nguyên liệu đã bị hạn chế", HttpStatus.CONFLICT),
    INGREDIENT_INVALID_NAME("Tên nguyên liệu không hợp lệ hoặc không được để trống", HttpStatus.UNPROCESSABLE_CONTENT),
    INGREDIENT_ALREADY_EXISTS("Tên nguyên liệu này đã tồn tại trong hệ thống", HttpStatus.CONFLICT),

    TAG_NOT_FOUND("Không tìm thấy tag hoặc tag đã bị hạn chế", HttpStatus.CONFLICT),
    TAG_INVALID_NAME("Tên tag không hợp lệ hoặc không được để trống", HttpStatus.UNPROCESSABLE_CONTENT),
    TAG_ALREADY_EXISTS("Tên tag này đã tồn tại trong hệ thống", HttpStatus.CONFLICT),

    MEAL_NOT_FOUND("Không tìm thấy món ăn", HttpStatus.CONFLICT),
    MEAL_ALREADY_EXISTS("Tên món ăn này đã tồn tại", HttpStatus.CONFLICT),
    MEAL_INVALID_PRICE("Giá tối đa không được nhỏ hơn giá tối thiểu", HttpStatus.UNPROCESSABLE_CONTENT),

    // Auth errors
    USER_ALREADY_EXISTS("Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("Email đã được sử dụng", HttpStatus.CONFLICT),
    USER_NOT_FOUND("Không tìm thấy tài khoản", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    PASSWORD_MISMATCH("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    ACCOUNT_INACTIVE("Tài khoản bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    OTP_INVALID("Mã OTP không đúng hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND("Không tìm thấy role phù hợp", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND("Không tìm thấy quyền phù hợp", HttpStatus.NOT_FOUND),
    ROLE_HAS_NO_PERMISSION("Quyền muốn xóa không tồn tại trong Role này", HttpStatus.NOT_FOUND),
    CANNOT_DELETE_ASSIGN_PERMISSION("Không thể xóa đi quyền thêm quyền cho role", HttpStatus.FORBIDDEN),

    // Subscription errors
    BUNDLE_NOT_FOUND("Không tìm thấy gói dịch vụ hoặc gói dịch vụ đã bị vô hiệu hóa", HttpStatus.NOT_FOUND),
    DURATION_INVALID("Thời hạn gói không hợp lệ", HttpStatus.BAD_REQUEST),
    ;
    ErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String message;
    private final HttpStatus statusCode;
}