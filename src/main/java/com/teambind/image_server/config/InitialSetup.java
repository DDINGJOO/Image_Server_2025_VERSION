package com.teambind.image_server.config;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class InitialSetup {
	public static final Map<String, ReferenceType> ALL_REFERENCE_TYPE_MAP = new ConcurrentHashMap<>();
	public static final Map<String, Extension> EXTENSION_MAP = new ConcurrentHashMap<>();
	public static final Map<String, ReferenceType> MONO_IMAGE_REFERENCE_TYPE_MAP = new ConcurrentHashMap<>();
	public static final Map<String, ReferenceType> MULTI_IMAGE_REFERENCE_TYPE_MAP = new ConcurrentHashMap<>();
	private final ReferenceTypeRepository referenceTypeRepository;
	private final ExtensionRepository extensionRepository;
	
	@PostConstruct
	public void init() {
		
		log.info("InitialSetup: Loading data on application startup...");
		loadData();
		log.info("InitialSetup: Data loaded successfully. EXTENSION_MAP size={}, REFERENCE_TYPE_MAP size={}",
				EXTENSION_MAP.size(), ALL_REFERENCE_TYPE_MAP.size());
	}
	
	
	@Scheduled(cron = "0 30 0 * * *")
	public void loadData() {
		loadExtension();
		loadReferenceType();

	}
	
	private void loadReferenceType() {
		synchronized (ALL_REFERENCE_TYPE_MAP) {
			ALL_REFERENCE_TYPE_MAP.clear();
			referenceTypeRepository.findAll().forEach(r -> {
				ALL_REFERENCE_TYPE_MAP.put(r.getCode(), r);
				if (r.getAllowsMultiple()) {
					MULTI_IMAGE_REFERENCE_TYPE_MAP.put(r.getCode(), r);
				} else {
					MONO_IMAGE_REFERENCE_TYPE_MAP.put(r.getCode(), r);
				}
			});
		}
	}
	
	private void loadExtension() {
		synchronized (EXTENSION_MAP) {
			EXTENSION_MAP.clear();
			extensionRepository.findAll().forEach(e -> {
				EXTENSION_MAP.put(e.getCode(), e);
			});
		}
	}
	
	
}
