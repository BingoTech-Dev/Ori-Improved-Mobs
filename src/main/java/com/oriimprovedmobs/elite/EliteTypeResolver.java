package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;

public final class EliteTypeResolver {
    private EliteTypeResolver() {}

    public static EnumSet<EliteType> resolve(EliteSpawnContext context, Set<EliteType> enabled) {
        EnumSet<EliteType> result = EnumSet.noneOf(EliteType.class);
        if (context.overworld() && context.night() && enabled.contains(EliteType.NIGHT_STALKER)) {
            result.add(EliteType.NIGHT_STALKER);
        }
        if (context.overworld() && context.night() && enabled.contains(EliteType.SHROUDED)) {
            result.add(EliteType.SHROUDED);
        }
        if (context.snowyBiome() && enabled.contains(EliteType.FROSTBORN)) {
            result.add(EliteType.FROSTBORN);
        }
        if (context.nether() && enabled.contains(EliteType.INFERNAL)) {
            result.add(EliteType.INFERNAL);
        }
        if (enabled.contains(EliteType.BREACHER)) {
            result.add(EliteType.BREACHER);
        }
        if (enabled.contains(EliteType.VAMPIRIC)) {
            result.add(EliteType.VAMPIRIC);
        }
        if (enabled.contains(EliteType.PATHFINDER)) {
            result.add(EliteType.PATHFINDER);
        }
        return result;
    }
}
