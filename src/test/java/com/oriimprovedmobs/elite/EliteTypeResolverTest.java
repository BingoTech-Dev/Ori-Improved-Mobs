package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteTypeResolverTest {
    @Test
    void resolvesNightStalkerAtOverworldNight() {
        var context = new EliteSpawnContext(true, false, false, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER),
                EliteTypeResolver.resolve(context, legacyTypes()));
    }

    @Test
    void resolvesFrostbornInSnowyBiome() {
        var context = new EliteSpawnContext(true, false, true, false);
        assertEquals(EnumSet.of(EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, legacyTypes()));
    }

    @Test
    void resolvesInfernalInNether() {
        var context = new EliteSpawnContext(false, true, false, false);
        assertEquals(EnumSet.of(EliteType.INFERNAL),
                EliteTypeResolver.resolve(context, legacyTypes()));
    }

    @Test
    void stacksNightAndFrostTraitsAtNightInSnowyBiome() {
        var context = new EliteSpawnContext(true, false, true, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, legacyTypes()));
    }

    @Test
    void respectsDisabledEliteTypesIndependently() {
        var context = new EliteSpawnContext(true, false, true, true);
        assertEquals(EnumSet.of(EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, EnumSet.of(EliteType.FROSTBORN)));
    }

    @Test
    void resolvesBreacherInEveryDimensionWhenEnabled() {
        var enabled = EnumSet.of(EliteType.BREACHER);
        var expected = EnumSet.of(EliteType.BREACHER);

        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(true, false, false, false), enabled));
        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(false, true, false, false), enabled));
        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(false, false, false, false), enabled));
    }

    @Test
    void returnsEmptyWhenNoEnvironmentMatches() {
        var context = new EliteSpawnContext(true, false, false, false);
        assertEquals(EnumSet.noneOf(EliteType.class),
                EliteTypeResolver.resolve(context, legacyTypes()));
    }

    private static EnumSet<EliteType> legacyTypes() {
        return EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.INFERNAL);
    }
}
