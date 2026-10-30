package com.shop.controller;

import java.time.Duration;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CartCookie {
    public static final String NAME = "SHOPEASE_CART";

    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NAME.equals(cookie.getName()) && valid(cookie.getValue())) return cookie.getValue();
            }
        }
        String token = UUID.randomUUID().toString();
        ResponseCookie persistentCookie = ResponseCookie.from(NAME, token)
                .httpOnly(true).secure(false).path("/").sameSite("Lax")
                .maxAge(Duration.ofDays(30)).build();
        response.addHeader("Set-Cookie", persistentCookie.toString());
        return token;
    }

    private boolean valid(String value) {
        if (value == null) return false;
        try { return UUID.fromString(value).toString().equals(value); }
        catch (IllegalArgumentException ignored) { return false; }
    }
}
