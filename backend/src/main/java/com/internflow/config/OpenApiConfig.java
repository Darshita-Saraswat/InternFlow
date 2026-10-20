package com.internflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI internFlowOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("InternFlow API")
						.version("v1")
						.description("REST API for the InternFlow student career-application workspace."));
	}
}
