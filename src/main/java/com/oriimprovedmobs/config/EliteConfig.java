package com.oriimprovedmobs.config;

import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraftforge.common.ForgeConfigSpec;

public final class EliteConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ForgeConfigSpec.BooleanValue NIGHT_STALKER_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FROSTBORN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue INFERNAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BREACHER_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHROUDED_ENABLED;
    public static final ForgeConfigSpec.BooleanValue VAMPIRIC_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PATHFINDER_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("elites");
        ENABLED = builder.define("enabled", EliteConfigDefaults.ENABLED);
        SPAWN_CHANCE = builder.defineInRange(
                "spawnChance", EliteConfigDefaults.SPAWN_CHANCE, 0.0D, 1.0D);
        NIGHT_STALKER_ENABLED = builder.define("nightStalkerEnabled", EliteConfigDefaults.NIGHT_STALKER_ENABLED);
        FROSTBORN_ENABLED = builder.define("frostbornEnabled", EliteConfigDefaults.FROSTBORN_ENABLED);
        INFERNAL_ENABLED = builder.define("infernalEnabled", EliteConfigDefaults.INFERNAL_ENABLED);
        BREACHER_ENABLED = builder.define("breacherEnabled", EliteConfigDefaults.BREACHER_ENABLED);
        SHROUDED_ENABLED = builder.define("shroudedEnabled", EliteConfigDefaults.SHROUDED_ENABLED);
        VAMPIRIC_ENABLED = builder.define("vampiricEnabled", EliteConfigDefaults.VAMPIRIC_ENABLED);
        PATHFINDER_ENABLED = builder.define("pathfinderEnabled", EliteConfigDefaults.PATHFINDER_ENABLED);
        builder.pop();
        SPEC = builder.build();
    }

    private EliteConfig() {}

    public static EnumSet<EliteType> enabledTypes() {
        return enabledTypes(
                NIGHT_STALKER_ENABLED.get(),
                FROSTBORN_ENABLED.get(),
                INFERNAL_ENABLED.get(),
                BREACHER_ENABLED.get(),
                SHROUDED_ENABLED.get(),
                VAMPIRIC_ENABLED.get(),
                PATHFINDER_ENABLED.get());
    }

    static EnumSet<EliteType> enabledTypes(
            boolean nightStalkerEnabled,
            boolean frostbornEnabled,
            boolean infernalEnabled,
            boolean breacherEnabled,
            boolean shroudedEnabled,
            boolean vampiricEnabled,
            boolean pathfinderEnabled) {
        EnumSet<EliteType> enabled = EnumSet.noneOf(EliteType.class);
        if (nightStalkerEnabled) {
            enabled.add(EliteType.NIGHT_STALKER);
        }
        if (frostbornEnabled) {
            enabled.add(EliteType.FROSTBORN);
        }
        if (infernalEnabled) {
            enabled.add(EliteType.INFERNAL);
        }
        if (breacherEnabled) {
            enabled.add(EliteType.BREACHER);
        }
        if (shroudedEnabled) {
            enabled.add(EliteType.SHROUDED);
        }
        if (vampiricEnabled) {
            enabled.add(EliteType.VAMPIRIC);
        }
        if (pathfinderEnabled) {
            enabled.add(EliteType.PATHFINDER);
        }
        return enabled;
    }
}
