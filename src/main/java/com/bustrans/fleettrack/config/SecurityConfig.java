package com.bustrans.fleettrack.config;

import com.bustrans.fleettrack.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/screen.png",
                                "/error", "/favicon.ico", "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/login", "/api/portal/login").permitAll()
                        // Students may update their own user record (fullName + phone only).
                        // Must appear before the admin-only rule; first match wins.
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("ADMIN", "STUDENT")
                        // All other user-management and dashboard endpoints are admin-only.
                        .requestMatchers("/api/users/**", "/api/dashboard/**").hasRole("ADMIN")
                        // Financial report is also accessible to Finance Officers.
                        .requestMatchers("/api/reports/financial").hasAnyRole("ADMIN", "FINANCE_OFFICER")
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        // Specific feedback rules — must appear before the blanket /api/feedback/** rule.
                        .requestMatchers(HttpMethod.GET, "/api/feedback/eligible", "/api/feedback/mine").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/feedback").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/feedback", "/api/feedback/summary").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/feedback/*/review").hasRole("ADMIN")
                        // Student-facing endpoints (admin has access too).
                        .requestMatchers("/api/trips/**", "/api/locations/**", "/api/bookings/**", "/api/students/**", "/api/feedback/**")
                                .hasAnyRole("STUDENT", "ADMIN")
                        // Specific POST rules before the general authenticated() catch-all for these groups
                        .requestMatchers(HttpMethod.POST, "/api/trip-statuses/**", "/api/location-updates/**").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/emergency-reports/**").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/crash-incidents/**").hasRole("DRIVER")
                        // PUT/DELETE on incident/report records are transport-officer workflow
                        .requestMatchers(HttpMethod.PUT, "/api/emergency-reports/**", "/api/crash-incidents/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers(HttpMethod.DELETE, "/api/emergency-reports/**", "/api/crash-incidents/**").hasRole("TRANSPORT_OFFICER")
                        // GET (and any remaining methods) require authentication; role filtering is in the controllers
                        .requestMatchers("/api/emergency-reports/**", "/api/crash-incidents/**", "/api/trip-statuses/**", "/api/location-updates/**").authenticated()
                        // Driver portal endpoints (no trailing 's' — distinct from admin /api/drivers/**)
                        .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        // Status workflow is write-only for transport officers; admin is read-only
                        .requestMatchers(HttpMethod.PUT, "/api/transport/emergency-reports/**", "/api/transport/crash-incidents/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers("/api/transport/**").hasAnyRole("TRANSPORT_OFFICER", "ADMIN")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        // Slip download: authenticated; role + ownership check is inside SlipController
                        .requestMatchers("/api/slips/**").authenticated()
                        .requestMatchers("/api/finance/**").hasAnyRole("FINANCE_OFFICER", "ADMIN")
                        // Bus management: writes are TRANSPORT_OFFICER only; reads are TRANSPORT_OFFICER + ADMIN.
                        // Specific method rules must appear before the general GET rule (first match wins).
                        .requestMatchers(HttpMethod.POST,   "/api/buses/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers(HttpMethod.PUT,    "/api/buses/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/buses/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers(HttpMethod.DELETE, "/api/buses/**").hasRole("TRANSPORT_OFFICER")
                        .requestMatchers(HttpMethod.GET,    "/api/buses/**").hasAnyRole("TRANSPORT_OFFICER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
