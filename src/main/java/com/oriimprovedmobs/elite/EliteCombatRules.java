package com.oriimprovedmobs.elite;

import java.util.Set;

public final class EliteCombatRules {
    private EliteCombatRules() {}

    public static EliteHitEffects onHit(Set<EliteType> types) {
        return new EliteHitEffects(
                types.contains(EliteType.FROSTBORN),
                types.contains(EliteType.INFERNAL));
    }
}
