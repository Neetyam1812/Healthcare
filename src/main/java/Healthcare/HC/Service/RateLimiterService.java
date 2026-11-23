package Healthcare.HC.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiting with Caffeine:
 * - max 5 failed logins per email+IP per 15 minutes
 * - max 3 OTP sends per email per hour
 */
@Service
public class RateLimiterService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final int MAX_OTP_SENDS = 3;

    // Cache key: email:ip -> failed attempts within 15 minutes
    private final Cache<String, AtomicInteger> loginFailureCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    // Cache key: email -> OTP send count within 1 hour
    private final Cache<String, AtomicInteger> otpSendCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(50_000)
            .build();

    public boolean isLoginBlocked(String email, String ip) {
        String key = buildLoginKey(email, ip);
        AtomicInteger count = loginFailureCache.getIfPresent(key);
        return count != null && count.get() >= MAX_FAILED_LOGINS;
    }

    public void recordFailedLogin(String email, String ip) {
        String key = buildLoginKey(email, ip);
        loginFailureCache.asMap().compute(key, (k, v) -> {
            if (v == null) {
                return new AtomicInteger(1);
            } else {
                v.incrementAndGet();
                return v;
            }
        });
    }

    public void resetFailedLogin(String email, String ip) {
        String key = buildLoginKey(email, ip);
        loginFailureCache.invalidate(key);
    }

    public boolean isOtpSendBlocked(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        AtomicInteger count = otpSendCache.getIfPresent(key);
        return count != null && count.get() >= MAX_OTP_SENDS;
    }

    public void recordOtpSend(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        otpSendCache.asMap().compute(key, (k, v) -> {
            if (v == null) {
                return new AtomicInteger(1);
            } else {
                v.incrementAndGet();
                return v;
            }
        });
    }

    private String buildLoginKey(String email, String ip) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "unknown";
        String normalizedIp = (ip != null) ? ip.trim() : "unknown";
        return normalizedEmail + ":" + normalizedIp;
    }
}
