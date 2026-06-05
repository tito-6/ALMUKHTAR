package com.mycompany.transfersystem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Caches request bodies so idempotency hashing and controllers can read the payload safely.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class IdempotencyRequestCacheFilter extends org.springframework.web.filter.OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String m = request.getMethod();
        if (("POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m))
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/api/")) {
            ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrapped, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
