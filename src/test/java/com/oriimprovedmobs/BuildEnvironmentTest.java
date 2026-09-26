package com.oriimprovedmobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildEnvironmentTest {
    @Test
    void testRuntimeUsesJava17() {
        assertEquals(17, Runtime.version().feature());
    }
}
