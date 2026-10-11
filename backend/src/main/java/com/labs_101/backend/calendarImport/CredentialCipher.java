package com.labs_101.backend.calendarImport;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts the passwords of the users' Apple accounts before they are stored, with AES-GCM and the key of
 * {@code calendar.encryption-key} (32 random bytes, base64 or hex). Without a valid key no Apple account can be
 * connected, the rest of the app still runs. A lost key means the accounts have to be connected again.
 */
@Component
public class CredentialCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public CredentialCipher(@Value("${calendar.encryption-key:}") String key) {
        this.key = parse(key);
    }

    private static SecretKeySpec parse(String key) {
        Logger logger = LoggerFactory.getLogger(CredentialCipher.class);
        if (key == null || key.isBlank()) {
            logger.info("calendar.encryption-key is not set, Apple accounts can't be connected");
            return null;
        }
        // quotes are part of the value in a properties file, in a .env file they aren't
        String value = key.strip().replaceAll("^[\"']|[\"']$", "");
        // openssl rand -hex 32, would be read as 48 bytes of base64 otherwise
        if (value.matches("[0-9a-fA-F]{64}")) {
            return new SecretKeySpec(HexFormat.of().parseHex(value), "AES");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            if (bytes.length == 32) {
                return new SecretKeySpec(bytes, "AES");
            }
            logger.error("calendar.encryption-key is {} bytes instead of 32, Apple accounts can't be connected. "
                    + "Create one with: openssl rand -base64 32 (or -hex 32)", bytes.length);
        } catch (IllegalArgumentException e) {
            logger.error("calendar.encryption-key is no base64, Apple accounts can't be connected. "
                    + "Create one with: openssl rand -base64 32");
        }
        return null;
    }

    public boolean isConfigured() {
        return key != null;
    }

    /** base64 of the random iv and the encrypted text. */
    public String encrypt(String text) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
            return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // another key than the one it was encrypted with
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private SecretKeySpec requireKey() {
        if (key == null) {
            throw new IllegalStateException("calendar.encryption-key is not set");
        }
        return key;
    }
}
