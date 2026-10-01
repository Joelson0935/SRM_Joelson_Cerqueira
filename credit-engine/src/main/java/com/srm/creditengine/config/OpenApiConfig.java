package com.srm.creditengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI creditEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SRM Credit Engine API")
                        .description("Plataforma de cessão de crédito multimoedas para operações de FIDC")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SRM Asset")
                                .email("tech@srmasset.com.br")));
    }
}
