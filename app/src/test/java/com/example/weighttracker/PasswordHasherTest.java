package com.example.weighttracker;

import org.junit.Test;

import static org.junit.Assert.*;

/** Unit tests for SHA-256 password hashing and verification. */
public class PasswordHasherTest {

    @Test
    public void hashPassword_producesColonSeparatedResult() {
        String hash = PasswordHasher.hashPassword("secret123");
        assertNotNull(hash);
        assertTrue(hash.contains(":"));
        assertEquals(2, hash.split(":").length);
    }

    @Test
    public void verifyPassword_correctPassword_returnsTrue() {
        String hash = PasswordHasher.hashPassword("myPassword");
        assertTrue(PasswordHasher.verifyPassword("myPassword", hash));
    }

    @Test
    public void verifyPassword_wrongPassword_returnsFalse() {
        String hash = PasswordHasher.hashPassword("myPassword");
        assertFalse(PasswordHasher.verifyPassword("wrongPassword", hash));
    }

    @Test
    public void hashPassword_nullInput_returnsNull() {
        assertNull(PasswordHasher.hashPassword(null));
    }

    @Test
    public void hashPassword_emptyInput_returnsNull() {
        assertNull(PasswordHasher.hashPassword(""));
    }

    @Test
    public void verifyPassword_nullStoredHash_returnsFalse() {
        assertFalse(PasswordHasher.verifyPassword("password", null));
    }

    @Test
    public void verifyPassword_nullPassword_returnsFalse() {
        String hash = PasswordHasher.hashPassword("password");
        assertFalse(PasswordHasher.verifyPassword(null, hash));
    }

    @Test
    public void hashPassword_samePlaintext_producesDifferentHashes() {
        String hash1 = PasswordHasher.hashPassword("samePassword");
        String hash2 = PasswordHasher.hashPassword("samePassword");
        assertNotEquals(hash1, hash2);
    }
}
