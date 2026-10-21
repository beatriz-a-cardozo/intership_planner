package com.interchangeplanner.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gerador de UUID versão 7 (RFC 9562): 48 bits de timestamp em ms + aleatoriedade.
 * Ordenável no tempo, o que mantém os índices B-tree compactos (inserções no fim),
 * diferente do UUIDv4 totalmente aleatório.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    static UUID generate(long epochMillis) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);

        bytes[0] = (byte) (epochMillis >>> 40);
        bytes[1] = (byte) (epochMillis >>> 32);
        bytes[2] = (byte) (epochMillis >>> 24);
        bytes[3] = (byte) (epochMillis >>> 16);
        bytes[4] = (byte) (epochMillis >>> 8);
        bytes[5] = (byte) epochMillis;
        bytes[6] = (byte) ((bytes[6] & 0x0F) | 0x70); // versão 7
        bytes[8] = (byte) ((bytes[8] & 0x3F) | 0x80); // variante RFC

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }
}
