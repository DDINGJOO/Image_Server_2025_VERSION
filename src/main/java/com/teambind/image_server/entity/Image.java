package com.teambind.image_server.entity;


import com.teambind.image_server.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {
	@Id
	@Column(name = "image_id")
	private String id; // 외부에서 발급하지 않으면 @GeneratedValue 고려
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ImageStatus status;
	
	@Column(name = "reference_id")
	private String referenceId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reference_type_id")
	private ReferenceType referenceType;
	
	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;
	
	@Column(name = "uploader_id")
	private String uploaderId;
	
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted;
	
	@OneToOne(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
	private StorageObject storageObject;
	
	@OneToMany(mappedBy = "image", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StatusHistory> statusHistories = new ArrayList<>();
	
	
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
