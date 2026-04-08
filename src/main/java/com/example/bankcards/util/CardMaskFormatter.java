package com.example.bankcards.util;

public final class CardMaskFormatter {

    private CardMaskFormatter() {
    }

    public static String maskLastFour(String lastFour) {
        return "**** **** **** " + lastFour;
    }
}
