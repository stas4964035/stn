package com.k44.stn.markers.service;

import com.k44.stn.common.time.SystemTimeProvider;
import com.k44.stn.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarkerServiceTest {
    @Test
    void expiresAt_calculated_from_fixed_time() {
        Instant fixed = Instant.parse("2025-01-01T12:00:00Z");
        Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

        TimeProvider timeProvider = new SystemTimeProvider(clock);

        Instant expiresAt = timeProvider.now().plusSeconds(600);

        assertEquals(Instant.parse("2025-01-01T12:10:00Z"), expiresAt);
    }
}
