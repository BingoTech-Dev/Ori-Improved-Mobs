package com.oriimprovedmobs.elite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteSpawnContextTest {
    @Test
    void identifiesVanillaNightWindowBoundaries() {
        assertFalse(EliteSpawnContext.from(true, false, false, 12_999L).night());
        assertTrue(EliteSpawnContext.from(true, false, false, 13_000L).night());
        assertTrue(EliteSpawnContext.from(true, false, false, 22_999L).night());
        assertFalse(EliteSpawnContext.from(true, false, false, 23_000L).night());
    }
}
