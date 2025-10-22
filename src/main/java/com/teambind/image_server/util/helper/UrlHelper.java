package com.teambind.image_server.util.helper;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlHelper {
	
	@Value("${images.base-url}")
	private String baseUrl;
	
	public String getUrl(String storedPath) {
		return baseUrl + storedPath;
	}
}
