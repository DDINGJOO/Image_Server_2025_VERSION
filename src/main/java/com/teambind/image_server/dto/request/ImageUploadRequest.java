package com.teambind.image_server.dto.request;

import com.teambind.image_server.util.validator.ValidImageFile;
import com.teambind.image_server.util.validator.ValidImageUpload;
import com.teambind.image_server.util.validator.ValidReferenceType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidImageUpload
public class ImageUploadRequest {

	@ValidImageFile
	private MultipartFile file;

	@ValidImageFile
	private List<MultipartFile> files;
	
	@NotBlank(message = "업로더 ID는 필수입니다")
	private String uploaderId;
	
	@NotBlank(message = "카테고리는 필수입니다")
	@ValidReferenceType
	private String category;
	
	/**
	 * 단일 이미지 업로드인지 확인
	 */
	public boolean isSingleUpload() {
		return file != null && !file.isEmpty();
	}
	
	/**
	 * 다중 이미지 업로드인지 확인
	 */
	public boolean isMultiUpload() {
		return files != null && !files.isEmpty();
	}
}
