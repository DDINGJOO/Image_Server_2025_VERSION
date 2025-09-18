package com.teambind.image_server;

import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j

public class ImageServerApplication implements ApplicationRunner {

    public static final Map<String, Integer> extensionMap = new HashMap<>();
    public static final Map<String, Integer> referenceTypeMap = new HashMap<>();
    private final ExtensionRepository extensionRepository;
    private final ReferenceTypeRepository referenceTypeRepository;
    public static void main(String[] args) {
        SpringApplication.run(ImageServerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        extensionRepository.findAll().forEach(extension -> {
            extensionMap.put(extension.getCode(), extension.getId());
        });
        log.info("Loaded {} extensions", extensionMap.toString());

        referenceTypeRepository.findAll().forEach(referenceType -> {
            referenceTypeMap.put(referenceType.getCode(), referenceType.getId());
        });
        log.info("Loaded {} reference types", referenceTypeMap.toString());

    }
}
