package com.teambind.image_server;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class ImageServerApplication {
	
	public static final Map<String, Extension> extensionMap = new HashMap<>();
	public static final Map<String, ReferenceType> referenceTypeMap = new HashMap<>();
	
	public static void main(String[] args) {
		SpringApplication.run(ImageServerApplication.class, args);
	}
}
