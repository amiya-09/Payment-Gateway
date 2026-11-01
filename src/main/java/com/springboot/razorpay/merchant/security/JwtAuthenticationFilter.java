package com.springboot.razorpay.merchant.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver  handlerExceptionResolver;
    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Intercept requests and validate JWT from the Authorization header.
        // If valid, set Authentication in SecurityContextHolder so the request is treated as authenticated.
        // If missing/invalid, let downstream handlers resolve the outcome.
        log.info("Incoming request: {}", request.getRequestURI());
        try {
            final String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.info("Invalid or missing authorization header");
                filterChain.doFilter(request, response);
                return;
            }
            String jwtToken = authorizationHeader.split("Bearer ")[1];
            // Header format: "Bearer <token>", so split to extract only the token value.
            // Validate token signature/claims before creating authentication.
            Claims claims = jwtUtil.verifyAccessToken(jwtToken);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("JWT token is valid. Claims: {}", claims);
                // Token is valid, so attach Authentication to the security context.
                // This lets downstream code treat the request as authenticated.
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(claims))));
                SecurityContextHolder.getContext().setAuthentication(auth);
                merchantContext.setMerchantId(UUID.fromString(jwtUtil.extractMerchantId(claims)));
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
    // Exceptions raised in the security filter chain do not automatically reach controller advice.
    // This filter runs before controllers, so @RestControllerAdvice cannot catch these directly.
    // HandlerExceptionResolver bridges this gap by routing security-layer exceptions into MVC handling.
    // That keeps error responses consistent with global exception handling.
}
