package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.util.InitialSetup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/enums")
public class enumsController {
	@GetMapping("/extensions")
	
	public ResponseEntity<Map<String, Extension>> getEnums() {
		return ResponseEntity.ok(InitialSetup.EXTENSION_MAP);
	}
	
	@GetMapping("/referenceType")
	public ResponseEntity<Map<String, ReferenceType>> getReferenceTypes() {
		return ResponseEntity.ok(InitialSetup.ALL_REFERENCE_TYPE_MAP);
	}
}
