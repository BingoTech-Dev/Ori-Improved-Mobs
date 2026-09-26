package com.oriimprovedmobs.elite;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteCooldownsTest {
    @Test
    void frostbornCooldownSurvivesNbtReloadAndExpiresAt100Ticks() {
        CompoundTag savedData = new CompoundTag();
        assertTrue(EliteCooldowns.isFrostbornRetaliationReady(savedData, 50L));
        EliteCooldowns.markFrostbornRetaliation(savedData, 50L);

        CompoundTag reloadedData = savedData.copy();
        assertFalse(EliteCooldowns.isFrostbornRetaliationReady(reloadedData, 149L));
        assertTrue(EliteCooldowns.isFrostbornRetaliationReady(reloadedData, 150L));
    }
}
