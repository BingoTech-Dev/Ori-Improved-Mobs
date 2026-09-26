package com.oriimprovedmobs.elite;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElitePresentationTest {
    @Test
    void labelKeysAreReturnedInStableEliteTypeOrder() {
        var types = Set.of(EliteType.FROSTBORN, EliteType.NIGHT_STALKER);

        assertEquals(List.of(
                "entity.ori_improved_mobs.elite.night_stalker",
                "entity.ori_improved_mobs.elite.frostborn"),
                ElitePresentation.labelKeys(types));
    }

    @Test
    void emptyEliteSetHasNoLabelKeys() {
        assertEquals(List.of(), ElitePresentation.labelKeys(Set.of()));
    }
}
