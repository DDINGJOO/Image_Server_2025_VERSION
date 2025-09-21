package com.teambind.image_server.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;



@ActiveProfiles("test")
@SpringBootTest
class ExtensionRepositoryDataJpaTest {

    @Autowired
    private  ExtensionRepository extensionRepository;



    @Test
    @DisplayName("확장자: WEBP/PNG/JPG 기본 데이터가 조회된다")
    void findAll_returnsSeedData() {
        var all = extensionRepository.findAll();
        assertThat(all).extracting("code").contains("WEBP", "PNG", "JPG");
    }
}
