package com.k44.stn.common.security;

import com.k44.stn.common.web.filter.RequestLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtBearerFilter jwtBearerFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, RequestLoggingFilter loggingFilter) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .anonymous(Customizer.withDefaults())
                .addFilterBefore(jwtBearerFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loggingFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
