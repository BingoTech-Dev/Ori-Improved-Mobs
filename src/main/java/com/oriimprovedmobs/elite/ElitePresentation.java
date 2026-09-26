package com.oriimprovedmobs.elite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ElitePresentation {
    private static final String ELITE_NAME_KEY = "entity.ori_improved_mobs.elite_name";

    private ElitePresentation() {}

    public static List<String> labelKeys(Set<EliteType> types) {
        List<String> keys = new ArrayList<>();
        for (EliteType type : EliteType.values()) {
            if (types.contains(type)) {
                keys.add(type.translationKey());
            }
        }
        return List.copyOf(keys);
    }

    public static Component name(Component mobName, Set<EliteType> types) {
        MutableComponent labels = Component.empty();
        boolean first = true;
        for (String key : labelKeys(types)) {
            if (!first) {
                labels.append(Component.literal(" + "));
            }
            labels.append(Component.translatable(key));
            first = false;
        }
        return Component.translatable(ELITE_NAME_KEY, labels, mobName);
    }
}
