package com.k44.stn.common.time;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@RequiredArgsConstructor
public class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    @Override
    public Instant now() {
        return Instant.now(clock);
    }
}
