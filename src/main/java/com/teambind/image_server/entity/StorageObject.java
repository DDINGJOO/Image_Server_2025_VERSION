package com.teambind.image_server.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "storage_objects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageObject {
	@Id
	@Column(name = "image_id")
	private String id; // Image와 공유 PK
	
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "image_id")
	private Image image;
	
	@Column(name = "storage_location", nullable = false, length = 1000)
	private String storageLocation;
	
	@Column(name = "origin_size", nullable = false)
	private long originSize;
	
	// NULL 가능 컬럼이므로 래퍼 타입 사용
	@Column(name = "converted_size")
	private Long convertedSize;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_format_id")
	private Extension originFormat;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "converted_format_id")
	private Extension convertedFormat;
}
