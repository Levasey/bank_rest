package com.example.bankcards.util;

import com.example.bankcards.config.AppSecurityProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PanEncryptionService {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PanEncryptionService(AppSecurityProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getEncryption().getKey());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.security.encryption.key must decode to 32 bytes (AES-256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, AES);
    }

    public String encrypt(String plainPan) throws GeneralSecurityException {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainPan.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
        buffer.put(iv);
        buffer.put(cipherText);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public String decrypt(String stored) throws GeneralSecurityException {
        byte[] combined = Base64.getDecoder().decode(stored);
        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] cipherBytes = new byte[buffer.remaining()];
        buffer.get(cipherBytes);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        byte[] plain = cipher.doFinal(cipherBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
