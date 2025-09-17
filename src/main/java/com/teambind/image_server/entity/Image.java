package com.teambind.image_server.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "images")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {
    @Id
    @Column(name = "image_id")
    String id;

    @Column(name = "reference_type_id")
    int referenceTypeId;

    @Column(name = "uploader_id")
    String uploaderId;
    @Column(name = "uploaded_at")
    LocalDateTime uploadedAt;
    @Column(name = "is_deleted")
    boolean isDeleted;



}
