package com.teambind.image_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageConfirmRequest {
	
	@NotBlank(message = "이미지 ID는 필수입니다")
	private String imageId;
	
	@NotBlank(message = "참조 ID는 필수입니다")
	private String referenceId;
}
