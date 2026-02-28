package com.k44.stn.common.security;

import com.k44.stn.common.web.filter.RequestLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.net.http.HttpClient;

@Configuration
public class SecurityConfig {

    @Bean
    public RequestLoggingFilter requestLoggingFilter(){
        return new RequestLoggingFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestLoggingFilter requestLoggingFilter
    ) throws Exception{
        http.httpBasic(Customizer.withDefaults());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/test").authenticated()
                .anyRequest().permitAll()
        );

        http.csrf(AbstractHttpConfigurer::disable);

        http.addFilterAfter(requestLoggingFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
