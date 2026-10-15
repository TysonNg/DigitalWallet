package com.tyson.digitalwallet.ddd.domain.exception;

/**
 * Ngoại lệ ném ra khi không thể lấy được Distributed Lock hoặc xảy ra xung đột đồng thời.
 */
public class DistributedLockException extends RuntimeException {

    public DistributedLockException(String message) {
        super(message);
    }

    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
