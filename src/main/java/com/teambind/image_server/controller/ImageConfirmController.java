package com.teambind.image_server.controller;


import com.teambind.image_server.service.ImageConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageConfirmController {
	private final ImageConfirmService imageConfirmService;
	
	
	@GetMapping("/confirm/{referenceId}")
	public ResponseEntity<Void> confirmImage(@RequestParam String imageId, @PathVariable(name = "referenceId") String referenceId) {
		imageConfirmService.confirmImage(imageId, referenceId);
		return ResponseEntity.ok().build();
	}
	
	@GetMapping("/confirm/{referenceId}")
	public ResponseEntity<Void> confirmImages(@RequestParam List<String> imageIds, @PathVariable(name = "referenceId") String referenceId) {
		imageConfirmService.confirmImages(imageIds, referenceId);
		return ResponseEntity.ok().build();
	}


}
