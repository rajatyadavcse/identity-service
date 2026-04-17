package com.microservice.LoginService.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Identity Service API", version = "1.0", description = "Authentication & User Management API for the Restaurant Microservices platform", contact = @Contact(name = "Identity Service Team")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "Paste your access token here (without the 'Bearer ' prefix)")
public class OpenApiConfig {

        @Value("${app.base-url:}")
        private String baseUrl;

        @Value("${RAILWAY_PUBLIC_DOMAIN:}")
        private String railwayDomain;

        @Value("${server.port:8080}")
        private String serverPort;

        /**
         * Tell SpringDoc to skip @AuthenticationPrincipal parameters.
         * Without this, SpringDoc tries to document the injected UserPrincipal
         * as a request parameter and throws an exception → 500 on /v3/api-docs.
         */
        @PostConstruct
        public void configureSpringDoc() {
                SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
        }

        @Bean
        public OpenAPI customOpenAPI() {
                List<Server> servers = new ArrayList<>();

                // Priority 1: Explicit base URL from APP_BASE_URL env var
                if (baseUrl != null && !baseUrl.isBlank()) {
                        servers.add(new Server()
                                        .url(baseUrl)
                                        .description("Production"));
                }
                // Priority 2: Auto-detect from Railway's built-in RAILWAY_PUBLIC_DOMAIN env var
                else if (railwayDomain != null && !railwayDomain.isBlank()) {
                        servers.add(new Server()
                                        .url("https://" + railwayDomain)
                                        .description("Production"));
                }

                // Always add localhost for local development
                servers.add(new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development"));

                return new OpenAPI()
                                .servers(servers);
        }

}
