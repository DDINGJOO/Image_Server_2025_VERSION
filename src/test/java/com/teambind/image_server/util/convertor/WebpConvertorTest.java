package com.teambind.image_server.util.convertor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;


class WebpConvertorTest {


    @Value("${image.dir}")
    private String Image_DIR;


    @Test
    @DisplayName("무손실 압축 테스트")
    void convertToWebpWithLosslessTEST() {
        // given
        String testFileName = "F_LOUNGE.png"; // 테스트 파일 명
        File testFile = new File(Image_DIR + testFileName);

        // when
        File convertedFile = webpConvertor.convertToWebpWithLossless("F_LOUNGE_LOSSLESS", testFile);

        // then
        double originalFileSizeKB = testFile.length() / 1024.0;
        double convertedFileSizeKB = convertedFile.length() / 1024.0;
        // 압축 비율 계산 (백분율)
        double compressionRatio = (convertedFileSizeKB / originalFileSizeKB) * 100;
        // 압축률 계산
        double compressionRate = 100 - compressionRatio;

        System.out.printf("Original File Size: %.2f KB%n", originalFileSizeKB);
        System.out.printf("Converted File Size: %.2f KB%n", convertedFileSizeKB);
        System.out.printf("Compression Rate: %.2f%%%n", compressionRate);

        assertTrue(convertedFile.exists(), "Converted file should exist");
    }

    @Test
    @DisplayName("손실 압축 테스트")
    void convertToWebpTEST() {
        // given
        String testFileName = "F_LOUNGE.png"; // 테스트 파일 명
        File testFile = new File(Image_DIR + testFileName);

        // when
        File convertedFile = photoService.convertToWebp("F_LOUNGE", testFile);

        // then
        double originalFileSizeKB = testFile.length() / 1024.0;
        double convertedFileSizeKB = convertedFile.length() / 1024.0;

        double compressionRatio = (convertedFileSizeKB / originalFileSizeKB) * 100; // 압축 비율 계산 (백분율)
        double compressionRate = 100 - compressionRatio; // 압축률 계산

        System.out.printf("Original File Size: %.2f KB%n", originalFileSizeKB);
        System.out.printf("Converted File Size: %.2f KB%n", convertedFileSizeKB);
        System.out.printf("Compression Rate: %.2f%%%n", compressionRate);

        assertTrue(convertedFile.exists(), "Converted file should exist");
    }


}
