package com.mysillydreams.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/", "/actuator/**", "/error").permitAll() // Permit root, actuators, error pages
                    .anyRequest().authenticated() // All other requests need authentication
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .defaultSuccessUrl("/api/user/me") // Redirect after successful login, can be changed
            )
            .oauth2ResourceServer(oauth2ResourceServer ->
                oauth2ResourceServer
                    .jwt() // Configure JWT validation
            )
            .logout(logout ->
                logout
                    .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        // The post_logout_redirect_uri should be configured in Keycloak client settings
        // and should be an allowed redirect URI.
        // Example: successHandler.setPostLogoutRedirectUri("http://localhost:8081/");
        // If not set, Keycloak might redirect to a default or show a generic page.
        // For robust logout, ensure the Keycloak client has a valid "Post Logout Redirect URI"
        // that points back to a safe page in your application (e.g., the login page or home page).
        // This URI must be registered in Keycloak's client settings under "Valid Post Logout Redirect URIs".
        // For now, we'll rely on Keycloak's default behavior or client-side configuration for post-logout redirection.
        // If you have a specific URI, uncomment and set it:
        // successHandler.setPostLogoutRedirectUri(issuerUri.replace("/realms/my-app-realm", "/realms/my-app-realm/account")); // Example, adjust as needed
        return successHandler;
    }
}
