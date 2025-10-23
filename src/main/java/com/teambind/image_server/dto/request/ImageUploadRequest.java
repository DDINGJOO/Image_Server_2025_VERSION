package com.teambind.image_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadRequest {
	@NotNull
	@ValidImageFile
	MultipartFile file;
	@NotEmpty
	@ValidImageFile
	List<MultipartFile> files;
	@NotBlank
	private String fileName;
	@NotBlank
	@ValidReferenceType
	private String category;
}
