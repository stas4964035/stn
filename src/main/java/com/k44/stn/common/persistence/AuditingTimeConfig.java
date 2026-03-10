package com.k44.stn.common.persistence;

import com.k44.stn.common.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class AuditingTimeConfig {
    private final TimeProvider timeProvider;

    @Bean(name ="auditingDateTimeProvider")
    public DateTimeProvider auditingDateTimeProvider(){
        return () -> (Optional.of(timeProvider.now()));
    }
}
