package com.springboot.razorpay.merchant.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@Setter
@Getter
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS) // This tells Spring Boot we will initialize this
// class later, but reference it from beans that are created earlier.
// So Spring injects a proxy now and resolves the real request-scoped object at runtime.
public class MerchantContext {
    private UUID merchantId;
    private String keyId;
}
