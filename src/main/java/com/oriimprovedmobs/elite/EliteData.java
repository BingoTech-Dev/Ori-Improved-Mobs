package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;

public final class EliteData {
    private static final String ELITE_TYPES_KEY = "ori_improved_mobs:elite_types";

    private EliteData() {}

    public static EnumSet<EliteType> read(Mob mob) {
        CompoundTag persistentData = mob.getPersistentData();
        if (!persistentData.contains(ELITE_TYPES_KEY, Tag.TAG_LIST)) {
            return EnumSet.noneOf(EliteType.class);
        }
        return EliteTypeCodec.read(persistentData.getList(ELITE_TYPES_KEY, Tag.TAG_STRING));
    }

    public static void write(Mob mob, Set<EliteType> types) {
        CompoundTag persistentData = mob.getPersistentData();
        if (types.isEmpty()) {
            persistentData.remove(ELITE_TYPES_KEY);
            return;
        }
        persistentData.put(ELITE_TYPES_KEY, EliteTypeCodec.write(types));
    }

    public static boolean isElite(Mob mob) {
        return !read(mob).isEmpty();
    }
}
