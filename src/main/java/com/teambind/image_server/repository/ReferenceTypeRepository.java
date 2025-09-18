package com.teambind.image_server.repository;


import com.teambind.image_server.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceTypeRepository extends JpaRepository<ReferenceType, Integer> {
}
