package com.teambind.image_server.util.store;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class LocalImageStorage {

    private final Path baseDir;

    // 생성자를 통해 baseDir를 Path 객체로 초기화합니다.
    public LocalImageStorage(@Value("${image.upload.dir}") String baseDirPath) {
        log.info("===== LocalImageStorage 생성자 호출됨 =====");
        log.info("application.yaml에서 전달된 image.upload.dir: {}", baseDirPath);
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
        log.info("초기화된 최종 baseDir 절대 경로: {}", this.baseDir);
        log.info("========================================");
    }


    public String store(MultipartFile file, String relativePath) {
        return storeBytes(getBytes(file), relativePath);
    }


    public String store(byte[] imageBytes, String relativePath) {
        return storeBytes(imageBytes, relativePath);
    }

    // 바이트 배열을 저장하는 공통 로직
    private String storeBytes(byte[] imageBytes, String relativePath) {
        // 1. 파일 이름에 경로 조작 문자가 있는지 확인합니다.
        if (relativePath.contains("..")) {

            //TODO EXCEPTION IMPL
            return null;
        }

        try {
            // 2. 최종 저장 경로를 계산하고 정규화합니다.
            Path targetPath = this.baseDir.resolve(relativePath).normalize();
            // --- ▼▼▼▼▼ 디버깅을 위한 핵심 로그 추가 ▼▼▼▼▼ ---
            log.info("---------- [경로 검증 디버그] ----------");
            log.info("baseDir        (기본 경로): {}", this.baseDir);
            log.info("relativePath   (상대 경로): {}", relativePath);
            log.info("targetPath     (최종 경로): {}", targetPath);
            log.info("검증 조건 (targetPath.startsWith(baseDir)): {}", targetPath.startsWith(this.baseDir));
            log.info("------------------------------------");
            // --- ▲▲▲▲▲ 로그 추가 끝 ▲▲▲▲▲ ---

            // 3. 최종 경로가 기본 디렉토리 내에 있는지 확인하여 경로 조작 공격을 방어합니다.
            if (!targetPath.startsWith(this.baseDir)) {
                log.error("경로 검증 실패! targetPath가 baseDir의 하위 경로가 아닙니다.");

                //TODO EXCEPTION IMPL
                return null;
            }

            // 4. 디렉토리를 생성하고 파일을 저장합니다.
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, imageBytes);

        } catch (IOException e) {
            //TODO EXCEPTION IMPL
            return null;
        }
        return relativePath;
    }


    public boolean delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")) {
            return false;
        }

        try {
            Path targetPath = this.baseDir.resolve(relativePath).normalize();
            if (targetPath.startsWith(this.baseDir)) {
                return Files.deleteIfExists(targetPath);
            }
            return false;
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: {}", relativePath, e);
            return false;
        }
    }


    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            //TODO EXCEPTION IMPL
            return null;
        }
    }
}
