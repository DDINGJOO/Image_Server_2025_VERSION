package com.teambind.image_server.entity;


import com.teambind.image_server.entity.key.ImageSequenceId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "image_sequence")
@IdClass(ImageSequenceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageSequence {
    @Id
    @Column(name = "id")
    private Long id;

    @Id
    @Column(name = "image_id")
    private String imageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private Image image;

    @Column(name = "seq_number", nullable = false)
    private Integer seqNumber;
}
