package com.teambind.image_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * <p>
 * 이미지 변환/저장을 백그라운드에서 처리하기 위한 Task Queue 설정
 *
 * @author Image Server Team
 * @since 3.0
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
	
	/**
	 * 이미지 처리 전용 ThreadPoolTaskExecutor
	 * <p>
	 * 설정:
	 * - 기본 스레드: 10개 (동시 처리 가능한 이미지 수)
	 * - 최대 스레드: 20개 (부하 시 증가)
	 * - 큐 용량: 500개 (대기 가능한 작업 수)
	 * - 거부 정책: CallerRunsPolicy (큐 초과 시 호출 스레드가 직접 처리)
	 *
	 * @return ThreadPoolTaskExecutor
	 */
	@Bean(name = "imageProcessingExecutor")
	public ThreadPoolTaskExecutor imageProcessingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		
		// 코어 스레드 풀 크기 (항상 유지되는 워커 스레드 수)
		executor.setCorePoolSize(10);
		
		// 최대 스레드 풀 크기 (부하 시 증가 가능한 최대 스레드 수)
		executor.setMaxPoolSize(20);
		
		// 큐 용량 (대기 가능한 작업 수)
		executor.setQueueCapacity(500);
		
		// 스레드 이름 접두사 (로그 추적 용이)
		executor.setThreadNamePrefix("image-worker-");
		
		// 스레드 유지 시간 (idle 스레드가 종료되기까지의 시간)
		executor.setKeepAliveSeconds(60);
		
		// 거부 정책: 큐가 가득 찼을 때 처리 방식
		// CallerRunsPolicy: 호출한 스레드(HTTP 요청 스레드)가 직접 처리
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		
		// 애플리케이션 종료 시 대기 중인 작업 완료 후 종료
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		
		executor.initialize();
		
		log.info("Image Processing Executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
				executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
		
		return executor;
	}
}
