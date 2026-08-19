package com.devtracker.support;

import java.util.Locale;

/** Keeps the application's email primary keys and authentication lookups consistent. */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
