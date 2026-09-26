package com.oriimprovedmobs.elite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class EliteCooldowns {
    private static final String FROSTBORN_LAST_RETALIATION_TICK_KEY =
            "ori_improved_mobs:frostborn_last_retaliation_tick";
    private static final long FROSTBORN_RETALIATION_COOLDOWN_TICKS = 100L;

    private EliteCooldowns() {}

    public static boolean isFrostbornRetaliationReady(CompoundTag persistentData, long gameTime) {
        return !persistentData.contains(FROSTBORN_LAST_RETALIATION_TICK_KEY, Tag.TAG_LONG)
                || gameTime - persistentData.getLong(FROSTBORN_LAST_RETALIATION_TICK_KEY)
                        >= FROSTBORN_RETALIATION_COOLDOWN_TICKS;
    }

    public static void markFrostbornRetaliation(CompoundTag persistentData, long gameTime) {
        persistentData.putLong(FROSTBORN_LAST_RETALIATION_TICK_KEY, gameTime);
    }
}
