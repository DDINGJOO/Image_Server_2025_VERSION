package com.teambind.image_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(name = "variant_code", nullable = false, length = 32) // e.g., THUMBNAIL, SMALL, MEDIUM
    private String variantCode;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean thumbnail;

    private String uploaderId;
    private LocalDateTime uploadedAt;
    private Integer width;
    private Integer height;
    private String url;
}
