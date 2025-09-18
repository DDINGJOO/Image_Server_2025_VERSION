package com.teambind.image_server.entity;


import com.teambind.image_server.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id")
    private Image image;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private ImageStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ImageStatus newStatus;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private String updatedBy;
    private String reason;
}
