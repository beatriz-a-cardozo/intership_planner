package com.interchangeplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.shared.domain.UuidV7;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void geraVersao7VarianteRfc() {
        UUID id = UuidV7.generate();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void prefixoDeTempoEhOrdenavel() throws InterruptedException {
        UUID first = UuidV7.generate();
        Thread.sleep(2);
        UUID second = UuidV7.generate();
        // Os 48 bits mais significativos são o timestamp em ms
        assertThat(second.getMostSignificantBits() >>> 16).isGreaterThan(first.getMostSignificantBits() >>> 16);
    }

    @Test
    void naoRepete() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(UuidV7.generate());
        }
        assertThat(new HashSet<>(ids)).hasSize(ids.size());
    }
}
