package com.k44.stn.common.time;

import java.time.Instant;

public interface TimeProvider {
    Instant now();
}
