package com.teambind.image_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reference_types", uniqueConstraints = @UniqueConstraint(name = "uk_reference_type_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 64)
    private String name;
}
