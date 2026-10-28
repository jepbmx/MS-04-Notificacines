package com.ecommerce.notificaciones.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI notificationApi() {
        return new OpenAPI().info(new Info().title("MS-04 Notificaciones").version("1.0")
                .description("Envio simulado sin proveedor externo. Lectura y entrega son independientes. Fechas UTC. Ejemplos y recorrido: README y docs/DEMOSTRACION.md."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer commonResponses() {
        return api -> api.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
            for (String code : new String[]{"400", "401", "403", "404", "409", "422", "500", "503"}) {
                op.getResponses().addApiResponse(code, new ApiResponse().description(
                        "Error controlado; ver contrato en README")
                        .content(new Content().addMediaType("application/json", new MediaType()
                                .schema(errorSchema())
                                .example(java.util.Map.of("timestamp", "2026-10-28T12:00:00Z",
                                        "status", Integer.parseInt(code), "code", "ERROR_CODE",
                                        "message", "Descripcion publica del error", "path", "/api/notifications")))));
            }
        }));
    }

    private Schema<?> errorSchema() {
        return new Schema<>().type("object")
                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                .addProperty("status", new Schema<>().type("integer"))
                .addProperty("code", new Schema<>().type("string"))
                .addProperty("message", new Schema<>().type("string"))
                .addProperty("path", new Schema<>().type("string"));
    }
}
