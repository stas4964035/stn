package com.k44.stn.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemTimeProviderTest {

    @Test
    void now_is_deterministic_with_fixed_clock() {
        Instant fixed = Instant.parse("2025-01-01T12:00:00Z");
        Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

        TimeProvider tp = new SystemTimeProvider(clock);

        assertEquals(fixed, tp.now());
        assertEquals(fixed, tp.now());
    }
}