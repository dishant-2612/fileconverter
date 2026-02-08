package com.project.fileconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileconverterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileconverterApplication.class, args);
	}

}
