package com.k44.stn.auth.domain;

public interface TokenVersionService {
    long initializeAndGet(Long userId);

    long getCurrent(Long userId);

    long incrementAndGet(Long userId);

    void invalidate(Long userId);
}
