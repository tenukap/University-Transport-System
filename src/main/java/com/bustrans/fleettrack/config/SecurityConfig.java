package com.bustrans.fleettrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Cross-Site Request Forgery) for testing purposes
                .csrf(csrf -> csrf.disable())

                // 2. Allow all requests without authentication
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/dashboard.html", "/api/**", "/css/**", "/js/**").permitAll()
                        .anyRequest().permitAll()
                )

                // 3. Disable the default login form and basic auth popup
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}