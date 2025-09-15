package com.ricash.ricash.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseTokendFilter firebaseTokendFilter;

    @Autowired
    public SecurityConfig(FirebaseTokendFilter firebaseTokendFilter) {
        this.firebaseTokendFilter = firebaseTokendFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permettre les endpoints publics EN PREMIER
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Autoriser les OPTIONS pour CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Routes spécifiques avec permissions
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/agents/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admins/register").permitAll()

                        // Routes admin seulement
                        .requestMatchers("/api/admin/**", "/api/admin/agents/validation").hasRole("ADMIN")

                        // Routes agent
                        .requestMatchers("/api/agent/**").hasAnyRole("AGENT", "ADMIN")

                        // Routes user
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "AGENT", "ADMIN")

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseTokendFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Supprimez le bean corsFilter() pour éviter les conflits
    // @Bean
    // public CorsFilter corsFilter() {
    //     ...
    // }
}