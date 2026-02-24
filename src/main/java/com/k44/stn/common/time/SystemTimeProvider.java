package com.k44.stn.common.time;

import java.time.Clock;
import java.time.Instant;

public class SystemTimeProvider implements TimeProvider{

    private final Clock clock;

    public SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return Instant.now(clock);
    }
}
