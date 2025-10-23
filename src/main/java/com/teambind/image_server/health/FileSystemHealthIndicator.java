package com.teambind.image_server.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 파일 시스템 헬스 체크
 * <p>
 * 이미지 업로드 디렉토리의 상태를 확인합니다:
 * - 디렉토리 존재 여부
 * - 읽기 권한
 * - 쓰기 권한
 * - 사용 가능한 공간
 */
@Component
@Slf4j
public class FileSystemHealthIndicator implements HealthIndicator {
	
	@Value("${images.upload.dir}")
	private String uploadPath;
	
	@Override
	public Health health() {
		try {
			Path path = Paths.get(uploadPath);
			
			// 디렉토리 존재 확인
			if (!Files.exists(path)) {
				return Health.down()
						.withDetail("path", uploadPath)
						.withDetail("reason", "Directory does not exist")
						.build();
			}
			
			// 디렉토리 여부 확인
			if (!Files.isDirectory(path)) {
				return Health.down()
						.withDetail("path", uploadPath)
						.withDetail("reason", "Path is not a directory")
						.build();
			}
			
			// 읽기 권한 확인
			if (!Files.isReadable(path)) {
				return Health.down()
						.withDetail("path", uploadPath)
						.withDetail("reason", "Directory is not readable")
						.build();
			}
			
			// 쓰기 권한 확인
			if (!Files.isWritable(path)) {
				return Health.down()
						.withDetail("path", uploadPath)
						.withDetail("reason", "Directory is not writable")
						.build();
			}
			
			// 사용 가능한 공간 확인
			long usableSpace = path.toFile().getUsableSpace();
			long totalSpace = path.toFile().getTotalSpace();
			double usagePercent = ((double) (totalSpace - usableSpace) / totalSpace) * 100;
			
			// 90% 이상 사용 중이면 경고
			if (usagePercent > 90) {
				return Health.down()
						.withDetail("path", uploadPath)
						.withDetail("usableSpaceGB", String.format("%.2f", usableSpace / (1024.0 * 1024.0 * 1024.0)))
						.withDetail("totalSpaceGB", String.format("%.2f", totalSpace / (1024.0 * 1024.0 * 1024.0)))
						.withDetail("usagePercent", String.format("%.2f%%", usagePercent))
						.withDetail("reason", "Disk space usage above 90%")
						.build();
			}
			
			return Health.up()
					.withDetail("path", uploadPath)
					.withDetail("usableSpaceGB", String.format("%.2f", usableSpace / (1024.0 * 1024.0 * 1024.0)))
					.withDetail("totalSpaceGB", String.format("%.2f", totalSpace / (1024.0 * 1024.0 * 1024.0)))
					.withDetail("usagePercent", String.format("%.2f%%", usagePercent))
					.build();
			
		} catch (Exception e) {
			log.error("FileSystem health check failed", e);
			return Health.down()
					.withDetail("path", uploadPath)
					.withException(e)
					.build();
		}
	}
}
