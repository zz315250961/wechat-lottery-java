package com.ruoyi.lottery.service;

import javax.servlet.http.Cookie;
import java.util.Optional;

public class SessionTokenCodec {
    public Optional<String> read(Cookie[] cookies, String cookieName) {
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
