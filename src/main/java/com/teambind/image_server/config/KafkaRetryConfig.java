package com.teambind.image_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Kafka 발행 재시도 설정
 * <p>
 * Kafka 발행 실패 시 자동으로 재시도하는 정책을 정의합니다.
 * - 최대 3번 재시도
 * - 지수 백오프 (1초, 2초, 4초)
 * - 일시적 네트워크 장애 대응
 */
@Configuration
@Slf4j
public class KafkaRetryConfig {
	
	@Bean
	public RetryTemplate kafkaRetryTemplate() {
		RetryTemplate retryTemplate = new RetryTemplate();
		
		// 재시도 정책: 최대 3번 시도
		ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();
		retryPolicy.setExceptionClassifier(new Classifier<Throwable, RetryPolicy>() {
			@Override
			public RetryPolicy classify(Throwable throwable) {
				// 모든 예외에 대해 재시도
				SimpleRetryPolicy policy = new SimpleRetryPolicy();
				policy.setMaxAttempts(3);
				return policy;
			}
		});
		
		// 백오프 정책: 지수 백오프
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(1000);   // 첫 재시도: 1초 대기
		backOffPolicy.setMultiplier(2.0);         // 매번 2배씩 증가
		backOffPolicy.setMaxInterval(10000);      // 최대 대기 시간: 10초
		
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(backOffPolicy);
		
		return retryTemplate;
	}
}
