package com.oriimprovedmobs.elite;

import java.util.Arrays;
import java.util.Optional;

public enum EliteType {
    NIGHT_STALKER("night_stalker"),
    FROSTBORN("frostborn"),
    INFERNAL("infernal");

    private final String id;

    EliteType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "entity.ori_improved_mobs.elite." + id;
    }

    public static Optional<EliteType> fromId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
