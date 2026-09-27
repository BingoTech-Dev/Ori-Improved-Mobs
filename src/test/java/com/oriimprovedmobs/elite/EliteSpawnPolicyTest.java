package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
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

        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.SHROUDED, EliteType.VAMPIRIC,
                EliteType.PATHFINDER, EliteType.BREACHER), result);
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

    @Test
    void rejectsRollAtTheSpawnChanceBoundary() {
        var context = new EliteSpawnContext(true, false, false, true);

        var result = EliteSpawnPolicy.select(
                true, true, false, false, true, context, EnumSet.allOf(EliteType.class),
                () -> 0.05D, 0.05D);

        assertTrue(result.isEmpty());
    }

    @Test
    void doesNotConsumeRollWhenNoEnvironmentMatches() {
        var context = new EliteSpawnContext(true, false, false, false);
        var rolls = new AtomicInteger();

        var result = EliteSpawnPolicy.select(
                true, true, false, false, true, context, legacyTypes(),
                () -> {
                    rolls.incrementAndGet();
                    return 0.0D;
                },
                0.05D);

        assertTrue(result.isEmpty());
        assertEquals(0, rolls.get());
    }

    @Test
    void oneSuccessfulRollAssignsEveryMatchingType() {
        var context = new EliteSpawnContext(true, false, true, true);
        var rolls = new AtomicInteger();

        var result = EliteSpawnPolicy.select(
                true, true, false, false, true, context, EnumSet.allOf(EliteType.class),
                () -> {
                    rolls.incrementAndGet();
                    return 0.049D;
                },
                0.05D);

        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.SHROUDED,
                EliteType.VAMPIRIC, EliteType.PATHFINDER, EliteType.BREACHER), result);
        assertEquals(1, rolls.get());
    }

    private static EnumSet<EliteType> legacyTypes() {
        return EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.INFERNAL);
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
