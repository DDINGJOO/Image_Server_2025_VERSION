package com.teambind.image_server.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SequentialImageResponse {
	private String imageFileName;
	private String imageId;
}
