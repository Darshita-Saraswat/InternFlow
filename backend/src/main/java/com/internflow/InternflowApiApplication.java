package com.internflow;

import java.util.TimeZone;

import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class InternflowApiApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(InternflowApiApplication.class, args);
	}

}
