package com.springboot.razorpay.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String randomBase64(int length) {
        // Whatever length we provide it is going to be the length of the byte array,
        // which will have random values, and then we will encode it to Base64, which will result in a longer string.
        // therefore the final string length will be longer due to Base64 encoding.
        // Base64 has characters : a-z, A-Z, 0-9, -, _ => total number of characters = 64 => 6 bits per character
        byte[] buf = new byte[length];
        SECURE_RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
