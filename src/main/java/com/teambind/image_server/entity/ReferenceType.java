package com.teambind.image_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "reference_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceType {

    @Id
    int id;

    @Column(name = "code")
    String code;
    @Column(name = "name")
    String name;
}
