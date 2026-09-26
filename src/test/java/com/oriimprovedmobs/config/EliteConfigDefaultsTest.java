package com.oriimprovedmobs.config;

import org.junit.jupiter.api.Test;

import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteConfigDefaultsTest {
    @Test
    void breacherConfigDefaultsToEnabledAndHasAnIndependentSwitch() {
        assertTrue(EliteConfigDefaults.BREACHER_ENABLED);
        assertTrue(EliteConfig.BREACHER_ENABLED.getDefault());
        assertEquals(EnumSet.allOf(EliteType.class), EliteConfig.enabledTypes(true, true, true, true));
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.INFERNAL),
                EliteConfig.enabledTypes(true, true, true, false));
    }

    @Test
    void defaultsEnableEachEliteAtFivePercent() {
        assertTrue(EliteConfigDefaults.ENABLED);
        assertEquals(0.05D, EliteConfigDefaults.SPAWN_CHANCE, 0.0D);
        assertTrue(EliteConfigDefaults.NIGHT_STALKER_ENABLED);
        assertTrue(EliteConfigDefaults.FROSTBORN_ENABLED);
        assertTrue(EliteConfigDefaults.INFERNAL_ENABLED);
        assertTrue(EliteConfigDefaults.BREACHER_ENABLED);
    }

    @Test
    void configSpecUsesTheDocumentedDefaults() {
        assertTrue(EliteConfig.ENABLED.getDefault());
        assertEquals(0.05D, EliteConfig.SPAWN_CHANCE.getDefault(), 0.0D);
        assertEquals(EnumSet.allOf(EliteType.class), EliteConfig.enabledTypes(true, true, true, true));
    }

    @Test
    void enabledTypesReturnsAnIndependentSet() {
        var first = EliteConfig.enabledTypes(true, true, true, true);
        first.clear();

        assertEquals(EnumSet.allOf(EliteType.class), EliteConfig.enabledTypes(true, true, true, true));
    }

    @Test
    void enabledTypesHonorEachPerTypeSwitch() {
        assertEquals(EnumSet.of(EliteType.FROSTBORN), EliteConfig.enabledTypes(false, true, false, false));
        assertEquals(EnumSet.noneOf(EliteType.class), EliteConfig.enabledTypes(false, false, false, false));
    }
}
