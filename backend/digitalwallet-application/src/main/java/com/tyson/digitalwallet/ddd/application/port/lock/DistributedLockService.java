package com.tyson.digitalwallet.ddd.application.port.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Port (Interface) cung cấp dịch vụ khóa phân tán trong hệ thống.
 * Được định nghĩa ở tầng Application để các UseCase tại tầng Application có thể sử dụng tường minh
 * mà không phụ thuộc vào thư viện cụ thể ở hạ tầng (Redisson/Redis).
 */
public interface DistributedLockService {

    /**
     * Thực thi tác vụ có bảo vệ bởi khóa phân tán.
     *
     * @param key       Khóa định danh tài nguyên cần khóa (ví dụ: "lock:wallet:transfer:123")
     * @param waitTime  Thời gian chờ tối đa để lấy khóa (0 = fail-fast)
     * @param leaseTime Thời gian tự động giải phóng khóa (tránh deadlock)
     * @param unit      Đơn vị thời gian
     * @param task      Tác vụ cần thực thi khi đã giữ khóa
     * @param <T>       Kiểu dữ liệu trả về của tác vụ
     * @return Kết quả trả về của tác vụ
     */
    <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task);

    /**
     * Tiện ích thực thi tác vụ với cấu hình mặc định (waitTime = 0, leaseTime = 10 giây).
     */
    default <T> T executeWithLock(String key, Supplier<T> task) {
        return executeWithLock(key, 0, 10, TimeUnit.SECONDS, task);
    }

    /**
     * Thực thi tác vụ dạng Runnable (không trả về kết quả).
     */
    void executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task);

    /**
     * Tiện ích thực thi Runnable với cấu hình mặc định (waitTime = 0, leaseTime = 10 giây).
     */
    default void executeWithLock(String key, Runnable task) {
        executeWithLock(key, 0, 10, TimeUnit.SECONDS, task);
    }
}
