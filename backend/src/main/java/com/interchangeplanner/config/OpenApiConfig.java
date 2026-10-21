package com.interchangeplanner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    static {
        // CurrentUser vem do JWT, não da requisição: não deve aparecer como parâmetro na documentação.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI openApi(AppProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.name() + " API")
                        .version("v1")
                        .description("API de planejamento financeiro e de intercâmbio. "
                                + "Autentique em /api/auth/login e use o accessToken como Bearer."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Servidor relativo: o contrato versionado não depende de host/porta de quem o gerou.
                .servers(List.of(new Server().url("/")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Convenção de contrato: todo campo de um schema {@code *Response} é sempre enviado, então é marcado como
     * obrigatório (o TypeScript gerado não o trata como opcional). Campos que podem vir nulos devem ser
     * declarados com {@code @Schema(nullable = true)} / tipo {@code null} e ficam de fora.
     * Requests já recebem {@code required} das anotações de validação ({@code @NotNull}, {@code @NotBlank}).
     */
    @Bean
    public OpenApiCustomizer responseFieldsRequired() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (!name.endsWith("Response") || schema.getProperties() == null) {
                    return;
                }
                Map<String, Schema<?>> properties = castProperties(schema);
                List<String> required = new ArrayList<>();
                properties.forEach((property, propertySchema) -> {
                    if (!isNullable(propertySchema)) {
                        required.add(property);
                    }
                });
                Collections.sort(required);
                schema.setRequired(required);
            });
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Schema<?>> castProperties(Schema schema) {
        return (Map<String, Schema<?>>) schema.getProperties();
    }

    private static boolean isNullable(Schema<?> schema) {
        return Boolean.TRUE.equals(schema.getNullable())
                || (schema.getTypes() != null && schema.getTypes().contains("null"));
    }
}
