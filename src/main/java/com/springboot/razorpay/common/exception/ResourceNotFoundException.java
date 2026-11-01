package com.springboot.razorpay.common.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object identifier;

    // resourceName: the type of resource that was missing.
    // identifier: the lookup value used to find that resource.

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName+" not found with identifier: "+identifier);
        this.resourceName = resourceName;
        this.identifier = identifier;
    }
}
