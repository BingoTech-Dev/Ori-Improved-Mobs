package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteTypeCodecTest {
    @Test
    void roundTripsMultipleTypes() {
        var expected = EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN);

        assertEquals(expected, EliteTypeCodec.read(EliteTypeCodec.write(expected)));
    }

    @Test
    void roundTripsBreacherType() {
        var expected = EnumSet.of(EliteType.BREACHER);

        assertEquals(expected, EliteTypeCodec.read(EliteTypeCodec.write(expected)));
    }

    @Test
    void storesBreacherUsingStableId() {
        assertEquals("breacher", EliteType.BREACHER.id());
    }

    @Test
    void readsEmptyTypeList() {
        assertEquals(EnumSet.noneOf(EliteType.class), EliteTypeCodec.read(new ListTag()));
    }

    @Test
    void ignoresUnknownIds() {
        var tag = new ListTag();
        tag.add(StringTag.valueOf("removed_type"));
        tag.add(StringTag.valueOf("infernal"));

        assertEquals(EnumSet.of(EliteType.INFERNAL), EliteTypeCodec.read(tag));
    }

    @Test
    void storesNewAffixIdsUsingStableIds() {
        assertEquals("shrouded", EliteType.SHROUDED.id());
        assertEquals("vampiric", EliteType.VAMPIRIC.id());
        assertEquals("pathfinder", EliteType.PATHFINDER.id());
    }
}
