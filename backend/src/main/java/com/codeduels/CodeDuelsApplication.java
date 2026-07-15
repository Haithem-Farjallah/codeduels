package com.codeduels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CodeDuelsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeDuelsApplication.class, args);
	}

}
