package com.github.dimitryivaniuta.gateway.statement.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing SHA-256 digests in lowercase hexadecimal form.
 */
public final class Sha256Support {

    private Sha256Support() {
    }

    /**
     * Computes a digest for UTF-8 text.
     *
     * @param value input text.
     * @return lowercase hexadecimal digest.
     */
    public static String hex(final String value) {
        return hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes a digest for raw bytes.
     *
     * @param value input bytes.
     * @return lowercase hexadecimal digest.
     */
    public static String hex(final byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value);
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
