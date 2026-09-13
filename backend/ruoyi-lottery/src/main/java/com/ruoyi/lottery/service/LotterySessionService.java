package com.ruoyi.lottery.service;

import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class LotterySessionService {
    public static final String USER_COOKIE = "LOTTERY_USER_SESSION";
    private static final String PREFIX = "lottery:user:session:";
    private static final int TTL_SECONDS = 7 * 24 * 60 * 60;
    private final RedisCache redis;
    private final SessionTokenCodec codec = new SessionTokenCodec();
    private final SecureRandom random = new SecureRandom();

    public LotterySessionService(RedisCache redis) { this.redis = redis; }

    public void login(long userId, HttpServletRequest request, HttpServletResponse response) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.setCacheObject(PREFIX + token, userId, TTL_SECONDS, TimeUnit.SECONDS);
        response.addHeader("Set-Cookie", USER_COOKIE + "=" + token + "; Path=/; Max-Age=" + TTL_SECONDS + "; HttpOnly; SameSite=Lax" + (request.isSecure() ? "; Secure" : ""));
    }

    public Optional<Long> currentUser(HttpServletRequest request) {
        Optional<String> token = codec.read(request.getCookies(), USER_COOKIE);
        if (!token.isPresent()) return Optional.empty();
        Object value = redis.getCacheObject(PREFIX + token.get());
        if (value instanceof Number) return Optional.of(((Number)value).longValue());
        if (value instanceof String) return Optional.of(Long.parseLong((String)value));
        return Optional.empty();
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        codec.read(request.getCookies(), USER_COOKIE).ifPresent(token -> redis.deleteObject(PREFIX + token));
        response.addHeader("Set-Cookie", USER_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" + (request.isSecure() ? "; Secure" : ""));
    }
}
