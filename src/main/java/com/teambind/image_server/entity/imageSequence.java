package com.teambind.image_server.entity;


import jakarta.persistence.*;
import lombok.*;

@Table(name = "image_sequence")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class imageSequence {

    @Id
    int id;

    @Id
    @JoinColumn(name = "image_id")
    String imageId;

    @Column(name = "seq_number")
    int seqNumber;
}
