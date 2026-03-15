package com.cliniq.config;

import com.cliniq.security.CustomUserDetailsService;
import com.cliniq.security.JwtAuthenticationEntryPoint;
import com.cliniq.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        log.info("DaoAuthenticationProvider configured with CustomUserDetailsService and BCryptPasswordEncoder");
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
                .cors(cors -> cors.configurationSource(null))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/queue/display/**").permitAll()
                        .requestMatchers("/api/appointment-slots/available/**").permitAll()
                        .requestMatchers("/api/queue/manage/**").hasRole("DOCTOR")
                        .requestMatchers("/api/settings/**").hasRole("DOCTOR")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/day-schedules/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers("/api/day-schedules/**").hasRole("DOCTOR")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/holidays/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers("/api/holidays/**").hasRole("DOCTOR")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/day-overrides/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers("/api/day-overrides/**").hasRole("DOCTOR")
                        .requestMatchers("/api/appointment-slots/**").hasRole("DOCTOR")
                        .requestMatchers("/api/appointments/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers("/api/patients/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers("/api/queue/**").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
