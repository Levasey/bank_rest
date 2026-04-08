package com.example.bankcards.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.bankcards.config.AppSecurityProperties;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PanEncryptionServiceTest {

    @Test
    void encryptAndDecrypt_roundTrip() throws Exception {
        AppSecurityProperties props = new AppSecurityProperties();
        props.getEncryption().setKey(Base64.getEncoder().encodeToString(new byte[32]));
        PanEncryptionService service = new PanEncryptionService(props);
        String pan = "4111111111111111";
        assertEquals(pan, service.decrypt(service.encrypt(pan)));
    }
}
