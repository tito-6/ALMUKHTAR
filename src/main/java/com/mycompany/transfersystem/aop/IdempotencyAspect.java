package com.mycompany.transfersystem.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.idempotency.IdempotentExecutionResult;
import com.mycompany.transfersystem.exception.IdempotencyRequiredException;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.idempotency.IdempotencyService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@Aspect
@Component
@Order(50)
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final ObjectMapper idempotencyPayloadMapper;

    public IdempotencyAspect(IdempotencyService idempotencyService,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
        this.idempotencyPayloadMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Around("@annotation(require)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, RequireIdempotencyKey require) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attrs.getRequest();
        String rawKey = request.getHeader("Idempotency-Key");
        if (rawKey == null) {
            throw new IdempotencyRequiredException("Idempotency-Key header is required");
        }
        String idempotencyKey = rawKey.trim();
        if (idempotencyKey.length() < 16 || idempotencyKey.length() > 128) {
            throw new IdempotencyRequiredException("Idempotency-Key must be between 16 and 128 characters after trimming");
        }

        long userId = SecurityUtils.resolveUserId(userDetails, userRepository);
        String endpoint = resolveEndpointKey(request);
        String requestHash;
        try {
            requestHash = buildRequestHash(request, joinPoint, userId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Idempotency request hash serialization failed", e);
        }

        String keyFingerprint = IdempotencyService.sha256Hex(idempotencyKey).substring(0, 12);
        log.debug("idempotency endpoint={} userId={} keyFp={} requestHash={}", endpoint, userId, keyFingerprint, requestHash);

        IdempotentExecutionResult result = idempotencyService.execute(
                userId,
                endpoint,
                idempotencyKey,
                requestHash,
                () -> {
                    try {
                        return IdempotentExecutionResult.from(joinPoint.proceed());
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                });

        return result.toMvcReturnValue();
    }

    /**
     * Hash uses resolved {@link RequestBody} / {@link PathVariable} / {@link RequestPart} arguments (reliable with
     * Spring Security request wrappers) plus URI template variables and sorted query string.
     */
    private String buildRequestHash(HttpServletRequest request, ProceedingJoinPoint joinPoint, long userId)
            throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append('\n');
        sb.append(request.getRequestURI()).append('\n');
        String qs = request.getQueryString();
        if (qs != null && !qs.isBlank()) {
            sb.append(normalizeQuery(qs)).append('\n');
        }
        sb.append(userId).append('\n');
        appendUriTemplateVariables(request, sb);
        String fromArgs = hashAnnotatedPayloadArguments(joinPoint);
        if (!fromArgs.isEmpty()) {
            sb.append(fromArgs);
        } else {
            ContentCachingRequestWrapper caching = findContentCachingWrapper(request);
            drainRequestBodyForHashing(caching);
            sb.append(readCachedRawBody(caching));
        }
        return IdempotencyService.sha256Hex(sb.toString());
    }

    private void appendUriTemplateVariables(HttpServletRequest request, StringBuilder sb) throws JsonProcessingException {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map<?, ?> raw) || raw.isEmpty()) {
            return;
        }
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            sorted.put(String.valueOf(e.getKey()), e.getValue());
        }
        sb.append("pathVars:").append(idempotencyPayloadMapper.writeValueAsString(sorted)).append('\n');
    }

    private String hashAnnotatedPayloadArguments(ProceedingJoinPoint joinPoint) throws JsonProcessingException {
        if (!(joinPoint.getSignature() instanceof MethodSignature ms)) {
            return "";
        }
        Method method = ms.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        StringBuilder acc = new StringBuilder();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter p = parameters[i];
            if (p.isAnnotationPresent(RequestBody.class)) {
                acc.append("body:").append(p.getName()).append('=');
                if (args[i] == null) {
                    acc.append("null");
                } else {
                    acc.append(idempotencyPayloadMapper.writeValueAsString(args[i]));
                }
                acc.append('\n');
            } else if (p.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = p.getAnnotation(PathVariable.class);
                String name = (pv != null && !pv.value().isBlank()) ? pv.value() : p.getName();
                acc.append("pathArg:").append(name).append('=').append(args[i]).append('\n');
            } else if (p.isAnnotationPresent(RequestPart.class)) {
                RequestPart rp = p.getAnnotation(RequestPart.class);
                String name = (rp != null && !rp.name().isBlank()) ? rp.name()
                        : ((rp != null && !rp.value().isBlank()) ? rp.value() : p.getName());
                acc.append("part:").append(name).append('=');
                if (args[i] == null) {
                    acc.append("null");
                } else {
                    acc.append(idempotencyPayloadMapper.writeValueAsString(args[i]));
                }
                acc.append('\n');
            }
        }
        return acc.toString();
    }

    @Nullable
    private ContentCachingRequestWrapper findContentCachingWrapper(HttpServletRequest request) {
        ServletRequest current = request;
        for (int i = 0; i < 32 && current != null; i++) {
            if (current instanceof ContentCachingRequestWrapper ccr) {
                return ccr;
            }
            if (current instanceof ServletRequestWrapper wrapper) {
                ServletRequest inner = wrapper.getRequest();
                if (inner == current) {
                    break;
                }
                current = inner;
            } else {
                break;
            }
        }
        log.debug("No ContentCachingRequestWrapper in chain starting from {}", request.getClass().getName());
        return null;
    }

    private void drainRequestBodyForHashing(@Nullable ContentCachingRequestWrapper w) {
        if (w == null) {
            return;
        }
        if (w.getContentAsByteArray().length > 0) {
            return;
        }
        try {
            w.getInputStream().readAllBytes();
        } catch (IOException ignored) {
            // leave body empty for hash
        }
    }

    private static String resolveEndpointKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("?")) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        return request.getMethod() + ":" + uri;
    }

    private static String normalizeQuery(String raw) {
        String[] pairs = raw.split("&");
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String p : pairs) {
            int i = p.indexOf('=');
            if (i < 0) {
                sorted.put(p, "");
            } else {
                sorted.put(p.substring(0, i), p.substring(i + 1));
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.toString();
    }

    private static String readCachedRawBody(@Nullable ContentCachingRequestWrapper w) {
        if (w == null) {
            return "";
        }
        byte[] buf = w.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return "";
        }
        return new String(buf, StandardCharsets.UTF_8);
    }
}
