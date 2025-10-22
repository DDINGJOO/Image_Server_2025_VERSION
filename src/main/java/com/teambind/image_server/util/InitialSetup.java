package com.teambind.image_server.util;

import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.repository.ExtensionRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InitialSetup {
	public static final Map<String, ReferenceType> ALL_REFERENCE_TYPE_MAP = new HashMap<>();
	public static final Map<String, Extension> EXTENSION_MAP = new HashMap<>();
	public static final Map<String, ReferenceType> MONO_IMAGE_REFERENCE_TYPE_MAP = new HashMap<>();
	public static final Map<String, ReferenceType> MULTI_IMAGE_REFERENCE_TYPE_MAP = new HashMap<>();
	private final ReferenceTypeRepository referenceTypeRepository;
	private final ExtensionRepository extensionRepository;
	
	@PostConstruct
	public void init() {
	
	}
	
	
	@Scheduled(cron = "0 30 0 * * *")
	public void setupReferenceType() {
		ALL_REFERENCE_TYPE_MAP.clear();
		MONO_IMAGE_REFERENCE_TYPE_MAP.clear();
		MULTI_IMAGE_REFERENCE_TYPE_MAP.clear();
		EXTENSION_MAP.clear();
		List<ReferenceType> referenceTypeList = referenceTypeRepository.findAll();
		referenceTypeList.forEach(r -> {
			ALL_REFERENCE_TYPE_MAP.put(r.getCode(), r);
			if (r.getAllowsMultiple()) {
				MULTI_IMAGE_REFERENCE_TYPE_MAP.put(r.getCode(), r);
			} else {
				MONO_IMAGE_REFERENCE_TYPE_MAP.put(r.getCode(), r);
			}
		});
		List<Extension> extensionList = extensionRepository.findAll();
		extensionList.forEach(e -> {
			EXTENSION_MAP.put(e.getCode(), e);
		});
	}
	
}
