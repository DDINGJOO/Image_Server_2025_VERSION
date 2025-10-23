package com.teambind.image_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageBatchConfirmRequest {
	
	@NotNull(message = "이미지 ID 리스트는 필수입니다")
	private List<@NotBlank(message = "이미지 ID는 빈 값일 수 없습니다") String> imageIds;
	
	@NotBlank(message = "참조 ID는 필수입니다")
	private String referenceId;
}
