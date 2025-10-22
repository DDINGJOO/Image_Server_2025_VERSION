package com.teambind.image_server.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "extensions", uniqueConstraints = @UniqueConstraint(name = "uk_extension_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Extension {
	
	@Id
	@Column(name = "code", nullable = false, length = 16)
	private String code;
	
	@Column(name = "name", nullable = false, length = 64)
	private String name;
}
