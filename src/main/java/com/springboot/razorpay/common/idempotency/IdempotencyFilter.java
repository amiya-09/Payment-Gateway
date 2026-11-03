package com.springboot.razorpay.common.idempotency;

import com.springboot.razorpay.common.exception.IdempotencyConflictException;
import com.springboot.razorpay.merchant.security.MerchantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {
        // We want this filter to work for POST,PUT and PATCH methods
    private static final Set<String> GUARDED_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(30);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final String SEPARATOR = "|";

    private final MerchantContext  merchantContext;
    private final IdempotencyStore idempotencyStore;
    private final HandlerExceptionResolver  handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if(!GUARDED_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader("X-Idempotency-Key");
        if(rawKey == null || rawKey.isBlank()) {
            log.warn("Idempotency key is missing for request: {} {}", request.getMethod(), request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        UUID merchantId = merchantContext.getMerchantId();
        String key = merchantId != null ? merchantId+":"+rawKey : rawKey;

        boolean claimed = idempotencyStore.setIfAbsent(key, IN_PROGRESS_TTL);
        if(!claimed) {
            // another thread has already claimed this key
            Optional<String> existing = idempotencyStore.get(key);
            if(existing.isPresent() && !IdempotencyStore.IN_PROGRESS.equals(existing.get())) {
                // The existing value is not in progress, but coming from the actual value stored in redis
                // so we can't use it
                log.warn("Existing value is not in progress for request: {} {}", request.getMethod(), request.getRequestURI());
                replay(request, response, existing.get());
            } else {
                log.warn("Idempotency key is already in progress for request: {} {}", request.getMethod(), request.getRequestURI());
                var ex = new IdempotencyConflictException("A request with this idempotency key is in progress");
                handlerExceptionResolver.resolveException(request, response, null, ex);
            }
            return;
        }
        // What if it is not claimed yet -- we are the first thread to enter this
        // first time claim
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response); // What this allows us to do is :
        // allows us to wrap the response so that we can read the response body and store it in the idempotency store
        try {
            chain.doFilter(request, wrapper);
        } finally {
            int status = wrapper.getStatus();
            byte[] bodyBytes = wrapper.getContentAsByteArray();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            if(status < 400 && bodyBytes.length > 0) {
                // Success - store the completed response for future replays
                String stored = status + SEPARATOR + body;
                idempotencyStore.store(key, stored, COMPLETED_TTL);
                log.debug("IdempotencyFilter: stored response status={} key={}" , status, key);
            } else {
                idempotencyStore.delete(key);
                log.debug("IdempotencyFilter: deleted key={} due to error response status={}", key, status);
            }
            // Always flush buffered body to the actual response.
            // If this is skipped the client recieves an empty body.
            wrapper.copyBodyToResponse();
        }

    }

    private void replay(HttpServletRequest request, HttpServletResponse response, String stored) throws IOException {
        int separatorIndex = stored.indexOf(SEPARATOR);
        if(separatorIndex < 0) {
            log.error("Invalid stored value: {}", stored);
            var ex = new IdempotencyConflictException("A request with this idempotency key is in progress");
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
        int status = Integer.parseInt(stored.substring(0, separatorIndex));
        String body = stored.substring(separatorIndex + 1);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
