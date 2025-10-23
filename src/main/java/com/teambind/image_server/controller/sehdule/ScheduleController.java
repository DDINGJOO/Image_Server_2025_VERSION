package com.teambind.image_server.controller.sehdule;


import com.teambind.image_server.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {
	
	private final ScheduleService scheduleService;
	
	
	// Crown Job By Api
	@GetMapping("/cleanup")
	public void cleanup() {
		scheduleService.cleanUpUnusedImages();
	}
	
}

