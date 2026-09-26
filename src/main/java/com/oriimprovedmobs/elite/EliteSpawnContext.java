package com.oriimprovedmobs.elite;

public record EliteSpawnContext(boolean overworld, boolean nether, boolean snowyBiome, boolean night) {
    public static EliteSpawnContext from(boolean overworld, boolean nether, boolean snowyBiome, long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, 24_000L);
        boolean night = timeOfDay >= 13_000L && timeOfDay < 23_000L;
        return new EliteSpawnContext(overworld, nether, snowyBiome, night);
    }
}
