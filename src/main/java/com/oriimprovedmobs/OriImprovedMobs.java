package com.oriimprovedmobs;

import com.oriimprovedmobs.config.EliteConfig;
import com.oriimprovedmobs.events.EliteCombatHandler;
import com.oriimprovedmobs.events.EliteJoinHandler;
import com.oriimprovedmobs.events.EliteSpawnHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(OriImprovedMobs.MOD_ID)
public final class OriImprovedMobs {
    public static final String MOD_ID = "ori_improved_mobs";

    public OriImprovedMobs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EliteConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new EliteSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new EliteJoinHandler());
        MinecraftForge.EVENT_BUS.register(new EliteCombatHandler());
    }
}
