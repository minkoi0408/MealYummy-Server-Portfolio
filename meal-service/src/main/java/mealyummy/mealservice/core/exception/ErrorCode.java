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
    ;
    ErrorCode(String message, HttpStatus statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String message;
    private final HttpStatus statusCode;
}