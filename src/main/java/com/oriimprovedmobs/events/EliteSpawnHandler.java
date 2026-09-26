package com.oriimprovedmobs.events;

import com.oriimprovedmobs.config.EliteConfig;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteSpawnContext;
import com.oriimprovedmobs.elite.EliteSpawnPolicy;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteSpawnHandler {
    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (mob.level().isClientSide) {
            return;
        }

        boolean naturalSpawn = event.getSpawnType() == MobSpawnType.NATURAL;
        boolean monsterCategory = mob.getType().getCategory() == MobCategory.MONSTER
                && "minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace());
        boolean boss = mob.getType().is(Tags.EntityTypes.BOSSES);
        boolean alreadyElite = EliteData.isElite(mob);

        EliteSpawnContext context = EliteSpawnContext.from(
                mob.level().dimension().equals(Level.OVERWORLD),
                mob.level().dimension().equals(Level.NETHER),
                mob.level().getBiome(mob.blockPosition()).is(Tags.Biomes.IS_SNOWY),
                mob.level().getDayTime());

        Random random = new Random(mob.getUUID().getMostSignificantBits()
                ^ mob.getUUID().getLeastSignificantBits());
        EnumSet<EliteType> selected = EliteSpawnPolicy.select(
                naturalSpawn,
                monsterCategory,
                boss,
                alreadyElite,
                EliteConfig.ENABLED.get(),
                context,
                EliteConfig.enabledTypes(),
                random::nextDouble,
                EliteConfig.SPAWN_CHANCE.get());

        if (!selected.isEmpty()) {
            EliteData.write(mob, selected);
        }
    }
}
