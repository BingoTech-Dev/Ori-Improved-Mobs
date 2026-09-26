package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteTypeResolverTest {
    @Test
    void resolvesNightStalkerAtOverworldNight() {
        var context = new EliteSpawnContext(true, false, false, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER),
                EliteTypeResolver.resolve(context, EnumSet.allOf(EliteType.class)));
    }

    @Test
    void resolvesFrostbornInSnowyBiome() {
        var context = new EliteSpawnContext(true, false, true, false);
        assertEquals(EnumSet.of(EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, EnumSet.allOf(EliteType.class)));
    }

    @Test
    void resolvesInfernalInNether() {
        var context = new EliteSpawnContext(false, true, false, false);
        assertEquals(EnumSet.of(EliteType.INFERNAL),
                EliteTypeResolver.resolve(context, EnumSet.allOf(EliteType.class)));
    }

    @Test
    void stacksNightAndFrostTraitsAtNightInSnowyBiome() {
        var context = new EliteSpawnContext(true, false, true, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, EnumSet.allOf(EliteType.class)));
    }
}
