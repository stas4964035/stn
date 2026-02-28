package com.k44.stn.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock utcClock(){
        return Clock.systemUTC();
    }

    @Bean
    public TimeProvider timeProvider(Clock utcClock){
        return new SystemTimeProvider(utcClock);
    }
}
