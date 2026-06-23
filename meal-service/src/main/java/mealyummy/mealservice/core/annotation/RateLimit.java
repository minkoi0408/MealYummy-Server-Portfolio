package mealyummy.mealservice.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu API cần giới hạn số lần gọi dựa trên Role của người dùng.
 * Yêu cầu phải đăng nhập (có SecurityContext) để hoạt động chính xác.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Tên key cho Redis (vd: "ai_prompt", "meal_generation").
     * Sẽ được ghép với username và ngày hiện tại.
     */
    String key();

    /**
     * Số lần tối đa được gọi trong ngày đối với tài khoản FREE.
     */
    int freeLimit() default 10;

    /**
     * Số lần tối đa được gọi trong ngày đối với tài khoản MEMBERSHIP.
     */
    int membershipLimit() default 50;
}
