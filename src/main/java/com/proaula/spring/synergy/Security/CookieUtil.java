package com.proaula.spring.synergy.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {

    @Value("${app.jwtCookieName}")
    private String jwtCookieName;

    @Value("${app.jwtRefreshCookieName}")
    private String jwtRefreshCookieName;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs}")
    private long jwtRefreshExpirationMs;

    public void createAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(jwtCookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // ⚠️ Cambiar a true en producción (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationMs / 1000));
        response.addCookie(cookie);
    }

    public void createRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(jwtRefreshCookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // ⚠️ Cambiar a true en producción (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtRefreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    public void clearCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(jwtCookieName, "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(jwtRefreshCookieName, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}
