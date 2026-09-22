package ca.cornalix.scoring.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Securise l'API avec des jetons JWT emis par Amazon Cognito -- le meme
 * User Pool que cornalix-ms-identity et cornalix-ms-diagnostic (SCRUM-1/
 * SCRUM-2) : une seule source d'identite pour toute la plateforme.
 *
 * Regles :
 *  - /actuator/health et /actuator/info restent ouverts (sondes de sante,
 *    load balancer -- ils ne doivent jamais dependre d'un jeton)
 *  - tout le reste exige un jeton valide : en-tete "Authorization: Bearer <jwt>"
 *
 * Le JwtDecoder ne contacte Cognito qu'au moment ou un jeton doit vraiment
 * etre verifie, jamais au demarrage de l'application.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${cornalix.cognito.region}") String region,
            @Value("${cornalix.cognito.user-pool-id}") String userPoolId) {

        String jwkSetUri = "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json"
                .formatted(region, userPoolId);

        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
