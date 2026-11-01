package com.springboot.razorpay.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String randomBase64(int length) {
        // 'length' defines random byte-array size, not final output string length.
        // Bytes are Base64 URL-encoded, so output becomes longer than input bytes.
        // Base64 URL alphabet has 64 chars (a-z, A-Z, 0-9, -, _), i.e., 6 bits per char.
        byte[] buf = new byte[length];
        SECURE_RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
