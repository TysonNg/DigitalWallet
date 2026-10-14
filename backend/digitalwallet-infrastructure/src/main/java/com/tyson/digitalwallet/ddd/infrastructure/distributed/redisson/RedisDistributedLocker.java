package com.tyson.digitalwallet.ddd.infrastructure.distributed.redisson;

import java.util.concurrent.TimeUnit;

public interface RedisDistributedLocker {

    // Here, only write the interface for distributed Lock
    // And please explain each function, in public.. read

    /**
     * Thử lấy khóa phân tán (Distributed Lock) trong khoảng thời gian chờ tối đa {@code waitTime}.
     * Nếu lấy được khóa thành công, khóa sẽ tự động giải phóng sau {@code leaseTime}.
     *
     * @param waitTime  Thời gian chờ tối đa để có được khóa
     * @param leaseTime Thời gian giữ khóa tối đa trước khi tự động giải phóng (tránh deadlock khi sự cố xảy ra)
     * @param unit      Đơn vị thời gian (ví dụ: TimeUnit.SECONDS, TimeUnit.MILLISECONDS)
     * @return {@code true} nếu lấy được khóa thành công trong thời gian waitTime; {@code false} nếu hết thời gian chờ mà chưa lấy được khóa
     * @throws InterruptedException nếu luồng hiện tại bị ngắt (interrupted) trong khi đang chờ lấy khóa
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * Lấy khóa phân tán và chặn (block) luồng hiện tại cho tới khi lấy được khóa thành công.
     * Khóa sẽ tự động giải phóng sau khoảng thời gian {@code leaseTime}.
     *
     * @param leaseTime Thời gian giữ khóa tối đa trước khi tự động hết hạn
     * @param unit      Đơn vị thời gian
     */
    void lock(long leaseTime, TimeUnit unit);

    /**
     * Giải phóng khóa phân tán (mở khóa).
     * Luồng gọi phương thức này cần phải là luồng đang nắm giữ khóa để đảm bảo an toàn.
     */
    void unlock();

    /**
     * Kiểm tra xem khóa này hiện tại có đang bị chiếm giữ bởi bất kỳ luồng/tiến trình nào trong hệ thống hay không.
     *
     * @return {@code true} nếu khóa đang ở trạng thái bị khóa (locked); ngược lại {@code false}
     */
    boolean isLocked();

    /**
     * Kiểm tra xem khóa phân tán có đang được nắm giữ bởi luồng cụ thể có định danh {@code threadId} hay không.
     *
     * @param threadId ID của luồng cần kiểm tra
     * @return {@code true} nếu luồng có threadId tương ứng đang giữ khóa; ngược lại {@code false}
     */
    boolean isHeldByThread(long threadId);

    /**
     * Kiểm tra xem khóa phân tán có đang được nắm giữ bởi chính luồng hiện tại (Current Thread) đang thực thi hay không.
     * Thường dùng để xác thực an toàn trước khi gọi {@link #unlock()}.
     *
     * @return {@code true} nếu luồng hiện tại đang sở hữu khóa; ngược lại {@code false}
     */
    boolean isHeldByCurrentThread();
}
