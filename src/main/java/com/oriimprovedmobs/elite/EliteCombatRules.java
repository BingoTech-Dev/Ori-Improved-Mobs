package com.oriimprovedmobs.elite;

import java.util.Set;

public final class EliteCombatRules {
    private EliteCombatRules() {}

    public static EliteHitEffects onHit(Set<EliteType> types) {
        return new EliteHitEffects(
                types.contains(EliteType.FROSTBORN),
                types.contains(EliteType.INFERNAL));
    }

    public static float attackDamageBonus(
            Set<EliteType> types,
            boolean meleeAttack,
            float targetHealth,
            float targetMaxHealth,
            boolean targetOnFire) {
        float bonus = 0.0F;
        if (meleeAttack
                && types.contains(EliteType.NIGHT_STALKER)
                && targetHealth < targetMaxHealth * 0.5F) {
            bonus += 0.15F;
        }
        if (types.contains(EliteType.INFERNAL) && targetOnFire) {
            bonus += 0.15F;
        }
        return bonus;
    }
}
