package com.teambind.image_server.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final RetryTemplate kafkaRetryTemplate;


	public void publish(String topic, Object message) {
		try {
			String json = objectMapper.writeValueAsString(message);
			
			// 재시도 로직 적용
			kafkaRetryTemplate.execute(
					context -> {
						// 재시도 시도
						try {
							log.debug("Attempting to publish to Kafka: topic={}, attempt={}",
									topic, context.getRetryCount() + 1);
							
							// 동기 방식으로 전송 결과 확인
							kafkaTemplate.send(topic, json).get(5, TimeUnit.SECONDS);
							
							log.info("Event published successfully: topic={}", topic);
							return null;
						} catch (Exception e) {
							log.warn("Kafka publish failed (attempt {}): topic={}, error={}",
									context.getRetryCount() + 1, topic, e.getMessage());
							throw new RuntimeException("Kafka send failed", e);
						}
					},
					context -> {
						// 모든 재시도 실패 시
						log.error("All retry attempts exhausted for Kafka publish: topic={}, totalAttempts={}",
								topic, context.getRetryCount());
						return null;
					}
			);

		} catch (JsonProcessingException e) {
			log.error("Failed to serialize message to JSON: topic={}", topic, e);
			throw new RuntimeException("Failed to serialize message to JSON", e);
		}
	}
}
