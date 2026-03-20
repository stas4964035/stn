package com.k44.stn.auth.application;

import com.k44.stn.auth.domain.TokenVersionService;
import com.k44.stn.common.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class RedisTokenVersionService implements TokenVersionService {

    private String key(Long userId){
        return "stn:" + appProperties.env() + ":auth:user-token-version:" + userId;
    }

    @Override
    public long initializeAndGet(Long userId) {
        redis.opsForValue().set(key(userId), 1L);
        return 1L;
    }

    @Override
    public long getCurrent(Long userId) {
        Long v = redis.opsForValue().get(key(userId));
        return v == null ? 1L : v;
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
    private final AppProperties appProperties;
}
