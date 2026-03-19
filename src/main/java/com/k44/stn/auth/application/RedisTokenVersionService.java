package com.k44.stn.auth.application;

import com.k44.stn.auth.domain.TokenVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTokenVersionService implements TokenVersionService {

    private String key(Long userId){
        // TODO: придумать как менять окружение ключа (dev|prod)
        return "stn:dev:auth:user-token-version:" + userId;
    }

    @Override
    public long initializeAndGet(Long userId) {
        redis.opsForValue().set(key(userId), 1L);
        return 1L;
    }

    @Override
    public long getCurrent(Long userId) {
        Long v = redis.opsForValue().get(key(userId));
        return v == null ? 0L : v;
    }

    @Override
    public long incrementAndGet(Long userId) {
        return redis.opsForValue().increment(key(userId));
    }

    @Override
    public void invalidate(Long userId) {
        redis.delete(key(userId));
    }

    private final RedisTemplate<String, Long> redis;
}
