package com.example.survivor;

import java.util.Locale;

/** Identifies exploration actions, excluding combat and treatment menus. */
final class SearchActionClassifier {
    private SearchActionClassifier() {}
    static boolean matches(String label) {
        if(label==null)return false;
        String value=label.toLowerCase(Locale.ROOT).trim();
        if(value.contains("тело") || value.contains("врагов") || value.contains("стрельбы"))return false;
        return value.startsWith("искать грибы") || value.contains("обыскать") || value.startsWith("осмотреть") || value.startsWith("проверить")
                || value.equals("быстрый поиск") || value.equals("осторожный поиск") || value.equals("тщательный поиск");
    }
}
