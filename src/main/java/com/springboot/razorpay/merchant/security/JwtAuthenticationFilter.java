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

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver  handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // This method is going to intercept all the requests and check if the request has a valid JWT token
        // in the Authorization header. If the token is valid, it will set the authentication in the SecurityContextHolder,
        // allowing the request to proceed. If the token is invalid or missing, it will return an unauthorized response.
        log.info("Incoming request: {}", request.getRequestURI());
        try {
            final String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.info("Invalid or missing authorization header");
                filterChain.doFilter(request, response);
                return;
            }
            String jwtToken = authorizationHeader.split("Bearer ")[1];
            // Authorization header looks like : Bearer amcwbfq4f9g.gmc2pxuucvuv.cruwcvbucqoa -> so when we split it -> {"Bearer ", "amcwbfq4f9g.gmc2pxuucvuv.cruwcvbucqoa"}
            // Now we want to validate if this token is valid or not
            Claims claims = jwtUtil.verifyAccessToken(jwtToken);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("JWT token is valid. Claims: {}", claims);
                // If the token is valid, we can set the authentication in the SecurityContextHolder
                // so that the request can proceed.
                // We can also set the user details in the SecurityContextHolder if needed.
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.extractRole(claims))));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
    // This can be our normal JWT authentication filter, but this is not production ready, because we have not handled
    // the exceptions here. When an exception occurs in this layer (inside the above filter chain), that does not get propagated to the spring MVC layer.
    // In that case it won't be caught by the Global exception handler that we have created because these two are two different planes.
    // Currently, our request is traveling inside the security filter chain. It has not propagated yet to the controller layer,
    // all of this is happening before the controller layer is even invoked, so that's why we have to somehow
    // transfer the exception from the security filter chain to the controller layer, so that it can be caught by the global exception handler.
    // We use HandlerExceptionResolver to resolve the exception and propagate it to the controller layer, so that it can be caught by the global exception handler.
}

