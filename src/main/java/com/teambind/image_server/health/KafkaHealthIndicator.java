package com.teambind.image_server.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 헬스 체크
 * <p>
 * Kafka 클러스터의 연결 상태를 확인합니다:
 * - Kafka 브로커 연결 가능 여부
 * - 클러스터 노드 수
 * - 클러스터 ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {
	
	private final KafkaAdmin kafkaAdmin;
	
	@Override
	public Health health() {
		try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
			
			// Kafka 클러스터 정보 조회 (타임아웃 5초)
			DescribeClusterOptions options = new DescribeClusterOptions()
					.timeoutMs(5000);
			
			var describeCluster = adminClient.describeCluster(options);
			
			// 노드 수 확인
			int nodeCount = describeCluster.nodes().get(5, TimeUnit.SECONDS).size();
			
			// 클러스터 ID 확인
			String clusterId = describeCluster.clusterId().get(5, TimeUnit.SECONDS);
			
			// Controller ID 확인
			var controller = describeCluster.controller().get(5, TimeUnit.SECONDS);
			
			return Health.up()
					.withDetail("clusterId", clusterId)
					.withDetail("nodeCount", nodeCount)
					.withDetail("controller", controller.idString())
					.build();
			
		} catch (Exception e) {
			log.error("Kafka health check failed", e);
			return Health.down()
					.withDetail("reason", "Failed to connect to Kafka cluster")
					.withDetail("error", e.getMessage())
					.build();
		}
	}
}
