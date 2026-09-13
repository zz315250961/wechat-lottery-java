package com.ruoyi.lottery.service;

import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LotterySessionServiceTest {
    @Test
    void logoutDeletesServerSessionAndExpiresSecureCookie() {
        RedisCache redis = mock(RedisCache.class);
        LotterySessionService sessions = new LotterySessionService(redis);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setCookies(new Cookie(LotterySessionService.USER_COOKIE, "session-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessions.logout(request, response);

        verify(redis).deleteObject("lottery:user:session:session-token");
        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
    }
}
