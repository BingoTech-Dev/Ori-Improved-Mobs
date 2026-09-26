package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public final class EliteTypeCodec {
    private EliteTypeCodec() {}

    public static ListTag write(Set<EliteType> types) {
        ListTag tag = new ListTag();
        for (EliteType type : EliteType.values()) {
            if (types.contains(type)) {
                tag.add(StringTag.valueOf(type.id()));
            }
        }
        return tag;
    }

    public static EnumSet<EliteType> read(ListTag tag) {
        EnumSet<EliteType> types = EnumSet.noneOf(EliteType.class);
        for (int index = 0; index < tag.size(); index++) {
            EliteType.fromId(tag.getString(index)).ifPresent(types::add);
        }
        return types;
    }
}
