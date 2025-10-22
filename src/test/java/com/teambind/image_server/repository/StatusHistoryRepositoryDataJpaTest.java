package com.teambind.image_server.repository;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.entity.StatusHistory;
import com.teambind.image_server.enums.ImageStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class StatusHistoryRepositoryDataJpaTest {
	
	@Autowired
	private StatusHistoryRepository statusHistoryRepository;
	@Autowired
	private ImageRepository imageRepository;
	@Autowired
	private ReferenceTypeRepository referenceTypeRepository;
	
	@BeforeEach
	void setUp() {
		statusHistoryRepository.deleteAll();
		imageRepository.deleteAll();
	}
	
	@AfterEach
	void tearDown() {
		statusHistoryRepository.deleteAll();
		imageRepository.deleteAll();
	}
	
	@Test
	@DisplayName("StatusHistory를 직접 저장/조회할 수 있다")
	void saveAndFind() {
		ReferenceType ref = referenceTypeRepository.findAll().stream()
				.filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();
		
		Image img = Image.builder()
				.id("img-his-save")
				.status(ImageStatus.TEMP)
				.referenceType(ref)
				.imageUrl("http://local/images/save.webp")
				.uploaderId("tester")
				.idDeleted(false)
				.createdAt(LocalDateTime.now())
				.build();
		imageRepository.save(img);
		
		StatusHistory h = StatusHistory.builder()
				.image(img)
				.oldStatus(ImageStatus.TEMP)
				.newStatus(ImageStatus.READY)
				.updatedAt(LocalDateTime.now())
				.updatedBy("SYSTEM")
				.reason("initial ready")
				.build();
		statusHistoryRepository.save(h);
		
		List<StatusHistory> list = statusHistoryRepository.findAll();
		assertThat(list).hasSize(1);
		StatusHistory got = list.get(0);
		assertThat(got.getImage().getId()).isEqualTo("img-his-save");
		assertThat(got.getOldStatus()).isEqualTo(ImageStatus.TEMP);
		assertThat(got.getNewStatus()).isEqualTo(ImageStatus.READY);
		assertThat(got.getReason()).isEqualTo("initial ready");
	}
}
