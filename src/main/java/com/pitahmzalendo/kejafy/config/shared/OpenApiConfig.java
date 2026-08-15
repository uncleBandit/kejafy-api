package com.pitahmzalendo.kejafy.config.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SESSION_COOKIE_SCHEME = "sessionCookie";

    @Bean
    public OpenAPI kejafyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KejaFy API")
                        .version("0.0.1")
                        .description("""
                                KejaFy backend API.

                                Auth note: endpoints under /api/v1/auth are protected by a \
                                session cookie set during OAuth2 login (Google), not a bearer \
                                token. Swagger UI's "Authorize" button here just documents that \
                                requirement — it can't complete Google's login flow itself. To \
                                actually call a protected endpoint from this page: log in via \
                                GET /oauth2/authorization/google in the same browser first, \
                                then use "Try it out" here; the session cookie will ride along \
                                automatically.
                                """))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session cookie set after completing OAuth2 login.")
                        )
                );
    }
}
