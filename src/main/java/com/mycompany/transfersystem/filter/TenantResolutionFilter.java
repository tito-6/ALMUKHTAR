package com.mycompany.transfersystem.filter;

import com.mycompany.transfersystem.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(TENANT_HEADER);
            Long tenantId = DEFAULT_TENANT_ID;
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    tenantId = Long.parseLong(tenantHeader.trim());
                } catch (NumberFormatException ignored) {
                    // keep default
                }
            }
            TenantContextHolder.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
