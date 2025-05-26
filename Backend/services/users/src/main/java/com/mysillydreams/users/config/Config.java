package com.mysillydreams.users.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080/auth")
                .realm("master")
                .username("admin")
                .password("<PASSWORD>")
                .clientId("admin-cli")
                .build();
    }
}
