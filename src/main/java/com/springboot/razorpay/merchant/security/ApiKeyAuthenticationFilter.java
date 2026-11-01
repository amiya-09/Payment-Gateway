package com.springboot.razorpay.merchant.security;


import com.springboot.razorpay.merchant.entity.ApiKey;
import com.springboot.razorpay.merchant.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String BASIC_PREFIX = "Basic ";
    private final ApiKeyRepository apiKeyRepository;
//    private final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private final PasswordEncoder passwordEncoder;
    private final MerchantContext merchantContext;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Intercept requests and validate API Key from the Authorization header.
        // If valid, set Authentication in SecurityContextHolder so the request is treated as authenticated.
        // If missing/invalid, let downstream handlers resolve the outcome.
        log.info("Incoming request: {}", request.getRequestURI());
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith(BASIC_PREFIX)) {
                log.info("Invalid or missing authorization header");
                filterChain.doFilter(request, response);
                return;
            }
            // Authorization: Basic key advesaffrvfvb:secret advreverbetnhnhny
            // Authorization: Basic AVBREVBREVBRVBUERBVERVBRBVERVBEBVIBEVIEIRV
            // Header format: "Basic <key>", so split to extract only the key value.
            // Validate API Key before creating authentication.
            String[] credentials = decode(authorizationHeader);
            if(credentials == null){
                throw new BadCredentialsException("Malformed API Key Header");
            }
            String keyId = credentials[0];
            String rawSecret = credentials[1];
            ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
                    .orElseThrow(() -> new BadRequestException("Invalid or missing API Key"));
            if(!apiKey.isEnabled() || !validateApiKey(apiKey, rawSecret)){
                throw new BadRequestException("Invalid or missing API Key");
            }
            log.info("API Key is valid. Key ID: {}", keyId);
            var auth = new UsernamePasswordAuthenticationToken(keyId, null,
                    List.of(new SimpleGrantedAuthority("API_KEY_ROLE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            merchantContext.setMerchantId(apiKey.getMerchant().getId());
            merchantContext.setKeyId(apiKey.getKeyId());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error during API Key authentication", e);
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    private boolean validateApiKey(ApiKey apiKey, String rawSecret) {
        // Implement your API key validation logic here
        // For example, you might check if the rawSecret matches the stored secret for the given apiKey
        if(passwordEncoder.matches(rawSecret, apiKey.getKeySecretHash())){
            return true;
        }
        boolean isInGracePeriod = apiKey.getGracePeriodExpiresAt() != null &&
                apiKey.getGracePeriodExpiresAt().isAfter(LocalDateTime.now());
        return isInGracePeriod
                && apiKey.getPreviousKeySecretHash() != null
                && passwordEncoder.matches(rawSecret, apiKey.getPreviousKeySecretHash());
    }

    private String[] decode(String authorizationHeader) {
        String encoded = authorizationHeader.substring(BASIC_PREFIX.length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        int colon = decoded.indexOf(':');
        if(colon < 1) return null;
        return new String[]{decoded.substring(0, colon), decoded.substring(colon + 1)};
    }
}
