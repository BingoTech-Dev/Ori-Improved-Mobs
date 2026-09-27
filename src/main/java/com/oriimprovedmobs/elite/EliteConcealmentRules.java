package com.oriimprovedmobs.elite;

import java.util.Set;

public final class EliteConcealmentRules {
    public static final float REVEAL_HEALTH_FRACTION = 0.5F;

    private EliteConcealmentRules() {}

    public static boolean shouldConceal(Set<EliteType> types, float effectiveHealth, float maxHealth) {
        return types.contains(EliteType.SHROUDED) && effectiveHealth >= maxHealth * REVEAL_HEALTH_FRACTION;
    }

    public static boolean shouldGlow(Set<EliteType> types, boolean concealed) {
        return !types.contains(EliteType.SHROUDED) || !concealed;
    }

    public static boolean shouldShowName(Set<EliteType> types, boolean concealed) {
        return types.contains(EliteType.SHROUDED) && !concealed;
    }
}
