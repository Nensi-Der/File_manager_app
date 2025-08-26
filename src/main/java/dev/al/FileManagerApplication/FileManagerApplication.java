package dev.al.FileManagerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@SpringBootApplication
public class FileManagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(FileManagerApplication.class, args);
	}
}
