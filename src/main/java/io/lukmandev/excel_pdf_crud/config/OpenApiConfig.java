package io.lukmandev.excel_pdf_crud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management API")
                        .description("REST API for managing employees — supports CRUD, salary filtering, soft/hard delete, Excel and PDF export.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Lukman Dev")
                                .email("lukman@lukmandev.io")));
    }
}
