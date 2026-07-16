package be.ephec.pdw.padel.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        security = @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
)
@SecurityScheme(
        name = OpenApiConfig.JWT_SCHEME_NAME,
        description = "Collez le token JWT retourne par /auth/login ou /auth/register",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
    public static final String JWT_SCHEME_NAME = "bearerAuth";
}
