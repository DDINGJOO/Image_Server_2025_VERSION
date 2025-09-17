package com.teambind.image_server.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "image_status")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageStatus {
    @Id
    @JoinColumn(name = "image_id")
    String imageId;

    @Enumerated(EnumType.STRING)
    ImageStatusEnum status;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "updated_by")
    String updater;
}
