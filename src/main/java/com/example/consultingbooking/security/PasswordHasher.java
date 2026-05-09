package com.example.consultingbooking.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {

    public static final String SALT = "CPT202-28";
    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "sha256$" + SALT + "$";
    private static final int SHA256_HEX_LENGTH = 64;

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        return PREFIX + toHex(digest(SALT + rawPassword));
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (isHashed(storedPassword)) {
            return constantTimeEquals(hash(rawPassword), storedPassword);
        }

        return constantTimeEquals(rawPassword, storedPassword);
    }

    public static boolean needsHash(String storedPassword) {
        return !isHashed(storedPassword);
    }

    private static boolean isHashed(String storedPassword) {
        return storedPassword != null
                && storedPassword.startsWith(PREFIX)
                && storedPassword.length() == PREFIX.length() + SHA256_HEX_LENGTH;
    }

    private static byte[] digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
