package com.teambind.image_server.util.statuschanger;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.ReferenceType;
import com.teambind.image_server.entity.StatusHistory;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.repository.ReferenceTypeRepository;
import com.teambind.image_server.repository.StatusHistoryRepository;
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
class StatusChangerSpringBootTest {
	
	@Autowired
	private StatusChanger statusChanger;
	@Autowired
	private ImageRepository imageRepository;
	@Autowired
	private ReferenceTypeRepository referenceTypeRepository;
	@Autowired
	private StatusHistoryRepository statusHistoryRepository;
	
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
	@DisplayName("상태 변경 시 StatusHistory가 올바른 old/new 값으로 저장된다")
	void changeStatus_savesCorrectHistory() {
		ReferenceType ref = referenceTypeRepository.findAll().stream()
				.filter(r -> r.getCode().equals("PROFILE")).findFirst().orElseThrow();
		
		Image image = Image.builder()
				.id("img-his-1")
				.status(ImageStatus.TEMP)
				.referenceType(ref)
				.imageUrl("http://local/images/h1.webp")
				.uploaderId("tester")
				.idDeleted(false)
				.createdAt(LocalDateTime.now())
				.build();
		imageRepository.save(image);
		
		// when: TEMP -> READY
		Image after1 = statusChanger.changeStatus(image, ImageStatus.READY);
		imageRepository.save(after1);
		
		// then: history 1건, old=TEMP, new=READY
		List<StatusHistory> histories1 = statusHistoryRepository.findAll();
		assertThat(histories1.get(0).getOldStatus()).isEqualTo(ImageStatus.TEMP);
		assertThat(histories1.get(0).getNewStatus()).isEqualTo(ImageStatus.READY);
		assertThat(histories1.get(0).getUpdatedBy()).isEqualTo("SYSTEM");
		
		// when: READY -> CONFIRMED
		Image after2 = statusChanger.changeStatus(after1, ImageStatus.CONFIRMED);
		imageRepository.save(after2);
		
		// then: history 2건 누적, 마지막 기록 검증
		List<StatusHistory> histories2 = statusHistoryRepository.findAll();
		assertThat(histories2).hasSize(2);
		StatusHistory last = histories2.stream()
				.max((a, b) -> a.getUpdatedAt().compareTo(b.getUpdatedAt()))
				.orElseThrow();
		assertThat(last.getOldStatus()).isEqualTo(ImageStatus.READY);
		assertThat(last.getNewStatus()).isEqualTo(ImageStatus.CONFIRMED);
	}
}
