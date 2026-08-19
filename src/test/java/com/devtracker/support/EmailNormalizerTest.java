package com.devtracker.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailNormalizerTest {

    @Test
    void normalizeTrimsAndLowerCasesEmail() {
        assertEquals("test@example.com", EmailNormalizer.normalize("  Test@EXAMPLE.Com "));
    }
}
