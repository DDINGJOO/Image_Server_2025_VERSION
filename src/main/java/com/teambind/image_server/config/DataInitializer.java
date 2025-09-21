package com.teambind.image_server.config;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.teambind.image_server.ImageServerApplication.extensionMap;
import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ExtensionRepository extensionRepository;
    private final ReferenceTypeRepository referenceTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        extensionRepository.findAll().forEach(extension -> extensionMap.put(extension.getCode(), extension));
        if (log.isInfoEnabled()) {
            extensionMap.values().forEach(ext -> log.info("확장자 로드: {}", ext.getName()));
        }

        referenceTypeRepository.findAll().forEach(ref -> referenceTypeMap.put(ref.getCode(), ref));
        if (log.isInfoEnabled()) {
            referenceTypeMap.values().forEach(ref -> log.info("레퍼런스 타입 로드: {}", ref.getName()));
        }
    }
}
