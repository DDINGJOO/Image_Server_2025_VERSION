package com.teambind.image_server.util.statuschanger;

import com.teambind.image_server.entity.Image;
import com.teambind.image_server.entity.StatusHistory;
import com.teambind.image_server.enums.ImageStatus;
import com.teambind.image_server.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class StatusChanger {
    // 히스토리 반영 + 스테이터스 변경

    private final StatusHistoryRepository statusHistoryRepository;


    public Image changeStatus(Image image, ImageStatus newStatus) {
        image.setStatus(newStatus);
        statusHistoryRepository.save(
                StatusHistory.builder()
                        .image(image)
                        .oldStatus(image.getStatus())
                        .newStatus(newStatus)
                        .updatedAt(LocalDateTime.now())
                        .updatedBy("SYSTEM")
                        .build()
        );
        return image;

    }
}
