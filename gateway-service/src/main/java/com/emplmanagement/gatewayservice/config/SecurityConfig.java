package com.emplmanagement.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfig()))
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/auth/**", "/login/**", "/actuator/**").permitAll()

                        // Dynamic routing paths - require authentication with role
                        .pathMatchers("/department-service/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/employee-service/**").hasAnyRole("USER", "ADMIN")

                        // All other exchanges require authentication
                        .anyExchange().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration cors = new CorsConfiguration();

        // Configure allowed origins - use specific origins in production
        cors.setAllowedOrigins(List.of("*"));

        // Configure allowed methods
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Configure allowed headers
        cors.setAllowedHeaders(List.of("*"));

        // Note: If you need credentials, replace "*" with specific origins
        cors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);

        return source;
    }

    /**
     * JWT Authentication Converter for reactive security
     */
    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Keycloak Role Converter - extracts roles from JWT token
     * Matches the logic used in downstream microservices
     */
    private static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm_access roles
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

            // Extract resource_access roles (client-specific)
            Collection<GrantedAuthority> resourceRoles = extractResourceRoles(jwt);

            // Combine all roles
            return Stream.concat(realmRoles.stream(), resourceRoles.stream())
                    .collect(Collectors.toList());
        }

        /**
         * Extract roles from realm_access claim
         */
        private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.get("roles") == null) {
                return List.of();
            }

            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> role.startsWith("ROLE_")
                            ? new SimpleGrantedAuthority(role)
                            : new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        /**
         * Extract roles from resource_access claim for specific client
         */
        private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) {
                return List.of();
            }

            // Extract roles specific to "bank-gateway" client
            Map<String, Object> clientResource = (Map<String, Object>) resourceAccess.get("ems-api");
            if (clientResource == null || clientResource.get("roles") == null) {
                return List.of();
            }

            List<String> roles = (List<String>) clientResource.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}