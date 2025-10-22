package com.teambind.image_server.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/health")
public class HealthController {
	@GetMapping
	public String health() {
		log.info("Health check");
		return "Server is up";
	}
}
