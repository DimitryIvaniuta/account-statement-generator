package com.github.dimitryivaniuta.gateway.statement.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Sha256Support}.
 */
class Sha256SupportTest {

    /**
     * Verifies deterministic digest calculation.
     */
    @Test
    void shouldComputeStableDigest() {
        assertThat(Sha256Support.hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
