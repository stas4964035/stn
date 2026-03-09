package com.k44.stn.common.security;

import com.k44.stn.common.web.filter.RequestLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

import java.util.Set;

@Configuration
public class SecurityConfig {

    @Bean
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestLoggingFilter requestLoggingFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http.httpBasic(Customizer.withDefaults());
        http.formLogin(Customizer.withDefaults());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/test").authenticated()
                .anyRequest().permitAll()
        );

        MediaTypeRequestMatcher jsonRequestMatcher = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);
        jsonRequestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        RequestHeaderRequestMatcher ajaxRequestMatcher = new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest");

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint, jsonRequestMatcher)
                .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint, ajaxRequestMatcher)
                .defaultAccessDeniedHandlerFor(restAccessDeniedHandler, jsonRequestMatcher)
                .defaultAccessDeniedHandlerFor(restAccessDeniedHandler, ajaxRequestMatcher)
        );

        http.csrf(AbstractHttpConfigurer::disable);

        http.addFilterAfter(requestLoggingFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
