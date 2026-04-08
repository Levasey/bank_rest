package com.example.bankcards.util;

public final class CardPanNormalizer {

    private CardPanNormalizer() {
    }

    public static String digitsOnly(String pan) {
        return pan == null ? "" : pan.replaceAll("\\D", "");
    }

    public static String lastFour(String digitsPan) {
        if (digitsPan == null || digitsPan.length() < 4) {
            throw new IllegalArgumentException("Card number must contain at least 4 digits");
        }
        return digitsPan.substring(digitsPan.length() - 4);
    }
}
