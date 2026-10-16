package com.tyson.digitalwallet.ddd.application.usecase.wallet.pin;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.cache.CacheService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WalletPinAttemptService {

    private static final String ATTEMPTS_PREFIX = "wallet:pin:attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final CacheService cacheService;

    public WalletPinAttemptService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void assertNotLocked(UUID walletId) {
        String key = ATTEMPTS_PREFIX + walletId;
        String val = cacheService.get(key);
        if (val != null) {
            try {
                int attempts = Integer.parseInt(val);
                if (attempts >= MAX_ATTEMPTS) {
                    Long remainingSeconds = cacheService.getExpire(key, TimeUnit.SECONDS);
                    long remainingMin = (remainingSeconds != null && remainingSeconds > 0)
                            ? Math.max(1, remainingSeconds / 60)
                            : LOCKOUT_MINUTES;
                    throw new AppException(ErrorCode.WALLET_PIN_LOCKED,
                            "Mã PIN bị tạm khóa do nhập sai quá 5 lần. Vui lòng thử lại sau " + remainingMin + " phút");
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public int recordFailedAttempt(UUID walletId) {
        String key = ATTEMPTS_PREFIX + walletId;
        Long attempts = cacheService.increment(key, 1);
        if (attempts != null && attempts == 1) {
            cacheService.expire(key, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        } else if (attempts != null && attempts >= MAX_ATTEMPTS) {
            cacheService.expire(key, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }

        int current = attempts != null ? attempts.intValue() : 1;
        return Math.max(0, MAX_ATTEMPTS - current);
    }

    public void resetAttempts(UUID walletId) {
        String key = ATTEMPTS_PREFIX + walletId;
        cacheService.delete(key);
    }
}
