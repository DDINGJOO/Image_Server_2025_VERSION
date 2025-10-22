package com.teambind.image_server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ImageServerApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(ImageServerApplication.class, args);
	}
}
