package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteSpawnPolicyTest {
    @Test
    void assignsMatchingTypeWhenNaturalSpawnRollSucceeds() {
        var context = new EliteSpawnContext(true, false, false, true);
        var result = EliteSpawnPolicy.select(
                true,
                true,
                false,
                false,
                true,
                context,
                EnumSet.allOf(EliteType.class),
                () -> 0.049D,
                0.05D);

        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER), result);
    }

    @Test
    void rejectsIneligibleSpawnCandidates() {
        var context = new EliteSpawnContext(true, false, false, true);
        var enabled = EnumSet.allOf(EliteType.class);

        assertTrue(select(false, true, false, false, true, context, enabled).isEmpty());
        assertTrue(select(true, false, false, false, true, context, enabled).isEmpty());
        assertTrue(select(true, true, true, false, true, context, enabled).isEmpty());
        assertTrue(select(true, true, false, true, true, context, enabled).isEmpty());
        assertTrue(select(true, true, false, false, false, context, enabled).isEmpty());
    }

    private static EnumSet<EliteType> select(
            boolean naturalSpawn,
            boolean monsterCategory,
            boolean boss,
            boolean alreadyElite,
            boolean globallyEnabled,
            EliteSpawnContext context,
            EnumSet<EliteType> enabled) {
        return EliteSpawnPolicy.select(naturalSpawn, monsterCategory, boss, alreadyElite, globallyEnabled,
                context, enabled, () -> 0.0D, 0.05D);
    }
}
