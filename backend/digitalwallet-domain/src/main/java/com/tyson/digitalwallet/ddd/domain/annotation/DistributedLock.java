package com.tyson.digitalwallet.ddd.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation dùng để đánh dấu phương thức cần được bảo vệ bởi Distributed Lock (Redis/Redisson).
 * Khóa được tự động lấy trước khi thực thi phương thức và giải phóng trong khối finally.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * Biểu thức SpEL động để sinh ra lock key.
     * Ví dụ: "'lock:wallet:transfer:' + #command.senderWalletId()"
     */
    String key();

    /**
     * Thời gian chờ lấy lock tối đa.
     * Mặc định = 0 (Fail-fast: từ chối ngay lập tức nếu lock đang bị giữ bởi tiến trình khác).
     */
    long waitTime() default 0;

    /**
     * Thời gian giữ lock tối đa trước khi tự động hết hạn (tránh deadlock khi sự cố xảy ra).
     * Mặc định là 10 giây.
     */
    long leaseTime() default 10;

    /**
     * Đơn vị thời gian cho waitTime và leaseTime.
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * Thông báo lỗi trả về cho client khi không lấy được lock.
     */
    String message() default "Giao dịch trước đó của bạn đang được xử lý, vui lòng chờ trong giây lát";
}
