package com.teambind.image_server.util.store;


import com.teambind.image_server.exception.CustomException;
import com.teambind.image_server.exception.ErrorCode;
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
    public LocalImageStorage(@Value("${images.upload.dir}") String baseDirPath) {
        log.info("===== LocalImageStorage 생성자 호출됨 =====");
        log.info("application.yaml에서 전달된 image.upload.dir: {}", baseDirPath);
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
        log.info("초기화된 최종 baseDir 절대 경로: {}", this.baseDir);

        // 기본 디렉토리 존재 확인 및 생성, 권한 체크
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            log.error("[이미지 저장소 초기화 실패] 디렉토리 생성 중 오류: path={}, message={}", this.baseDir, e.getMessage(), e);
        }
        try {
            boolean exists = Files.exists(this.baseDir);
            boolean isDir = Files.isDirectory(this.baseDir);
            boolean readable = Files.isReadable(this.baseDir);
            boolean writable = Files.isWritable(this.baseDir);
            log.info("[이미지 저장소 상태] exists={}, isDirectory={}, readable={}, writable={} (path={})",
                    exists, isDir, readable, writable, this.baseDir);
            if (!writable) {
                log.warn("[경고] 이미지 저장 경로에 쓰기 권한이 없습니다. 호스트 경로 권한(chmod/chown)과 Docker 볼륨 마운트 옵션을 확인하세요. path={}", this.baseDir);
            }
        } catch (Exception e) {
            log.warn("[이미지 저장소 권한 확인 실패] path={}, message={}", this.baseDir, e.getMessage());
        }
        log.info("========================================");
    }


    public String store(MultipartFile file, String relativePath) throws CustomException {
        return storeBytes(getBytes(file), relativePath);
    }


    public String store(byte[] imageBytes, String relativePath) throws CustomException {
        return storeBytes(imageBytes, relativePath);
    }

    // 바이트 배열을 저장하는 공통 로직
    private String storeBytes(byte[] imageBytes, String relativePath) throws CustomException {
        // 1. 파일 이름에 경로 조작 문자가 있는지 확인합니다.
        if (relativePath.contains("..")) {
            throw new CustomException(ErrorCode.INVALID_REFERENCE);
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
                throw new CustomException(ErrorCode.INVALID_REFERENCE);
            }

            // 4. 디렉토리를 생성하고 파일을 저장합니다.
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, imageBytes);

        } catch (IOException e) {
            log.error("[이미지 저장 실패] targetPath={}, baseDir={}, message={}",
                    this.baseDir.resolve(relativePath).normalize(), this.baseDir, e.getMessage(), e);
            throw new CustomException(ErrorCode.IOException);
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


    private byte[] getBytes(MultipartFile file) throws CustomException {
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.error("[이미지 바이트 추출 실패] filename={}, message={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new CustomException(ErrorCode.IOException);
        }
    }
}
