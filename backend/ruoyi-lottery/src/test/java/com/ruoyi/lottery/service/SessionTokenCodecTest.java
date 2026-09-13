package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;
import javax.servlet.http.Cookie;
import static org.junit.jupiter.api.Assertions.*;

class SessionTokenCodecTest {
    @Test
    void readsOnlyTheRequestedRoleCookie() {
        SessionTokenCodec codec = new SessionTokenCodec();
        Cookie[] cookies = { new Cookie("LOTTERY_ADMIN_SESSION", "admin"), new Cookie("LOTTERY_USER_SESSION", "user") };
        assertEquals("user", codec.read(cookies, "LOTTERY_USER_SESSION").orElse(null));
        assertFalse(codec.read(cookies, "LOTTERY_MERCHANT_SESSION").isPresent());
    }
}
