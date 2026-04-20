package com.example.weighttracker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** SHA-256 password hashing with per-user salts. Stored format: "base64(salt):base64(hash)". */
public final class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";

    private PasswordHasher() { }

    /** Hash a plain-text password. Returns null if password is null or empty. */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes());

            return Base64.getEncoder().encodeToString(salt)
                    + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Verify a plain-text password against a stored salt:hash string. */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        try {
            String[] parts = storedHash.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] actualHash = digest.digest(password.getBytes());

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
