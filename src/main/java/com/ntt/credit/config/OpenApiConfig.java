package com.ntt.credit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI creditOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Credit Service API")
                .version("v1")
                .description("Credit products and payments endpoints."));
  }
}
