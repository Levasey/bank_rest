package com.example.bankcards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Encryption encryption = new Encryption();

    public Jwt getJwt() {
        return jwt;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public static class Jwt {
        private String secret = "";
        private long expirationMs = 86400000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Encryption {
        private String key = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
