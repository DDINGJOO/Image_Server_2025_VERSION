package com.teambind.image_server.controller;


import com.teambind.image_server.dto.response.SequentialImageResponse;
import com.teambind.image_server.service.ImageSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageSaveController {
	private final ImageSaveService imageSaveService;
	
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> saveImage(@RequestParam("file") MultipartFile file,
	                                                     @RequestParam String uploaderId,
	                                                     @RequestParam String category) {
		Map<String, String> image = imageSaveService.saveImage(file, uploaderId, category);
		return ResponseEntity.ok().body(image);
	}
	
	@PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<List<SequentialImageResponse>> saveImages(@RequestParam("files") List<MultipartFile> files,
	                                                                @RequestParam String uploaderId,
	                                                                @RequestParam String category) {
		List<SequentialImageResponse> response = imageSaveService.saveImages(files, uploaderId, category);
		
		return ResponseEntity.ok().body(response);
	}
}
