package com.github.s3upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.github.s3upload.config")
public class S3uploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3uploadApplication.class, args);
	}

}
