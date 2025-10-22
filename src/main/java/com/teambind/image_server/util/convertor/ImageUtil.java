package com.teambind.image_server.util.convertor;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
public class ImageUtil {
	
	/**
	 * MultipartFile을 WebP 형식의 byte 배열로 변환합니다.
	 * Scrimage 라이브러리를 사용하여 안정적으로 이미지를 처리합니다.
	 *
	 * @param file    변환할 이미지 파일
	 * @param quality 변환 품질 (0.0f ~ 1.0f). WebpWriter의 품질(0~100)로 변환됩니다.
	 * @return WebP로 변환된 이미지의 byte 배열
	 * @throws IOException 이미지 처리 중 오류 발생 시
	 */
	public static byte[] toWebp(MultipartFile file, float quality) throws IOException {
		// 품질 값을 0-100 사이의 정수로 변환
		int webpQuality = (int) (quality * 100);
		
		// Scrimage를 사용하여 이미지를 읽고 WebP 바이트로 변환
		byte[] webpBytes = ImmutableImage.loader()
				.fromBytes(file.getBytes())
				.bytes(new WebpWriter().withQ(webpQuality));
		
		log.info("Scrimage를 사용하여 WebP 변환 완료: inputSize={}, outputSize={}",
				file.getSize(), webpBytes.length);
		
		return webpBytes;
	}
	
	/**
	 * MultipartFile을 지정된 사이즈의 WebP 썸네일로 변환합니다.
	 *
	 * @param file    변환할 이미지 파일
	 * @param width   썸네일 너비
	 * @param height  썸네일 높이
	 * @param quality 변환 품질 (0.0f ~ 1.0f)
	 * @return WebP로 변환된 썸네일 이미지의 byte 배열
	 * @throws IOException 이미지 처리 중 오류 발생 시
	 */
	public static byte[] toWebpThumbnail(MultipartFile file, int width, int height, float quality) throws IOException {
		int webpQuality = (int) (quality * 100);
		
		byte[] webpBytes = ImmutableImage.loader()
				.fromBytes(file.getBytes())
				.cover(width, height) // 지정된 크기로 이미지를 맞춤 (비율 유지, 중앙 크롭)
				.bytes(new WebpWriter().withQ(webpQuality));
		
		log.info("Scrimage를 사용하여 WebP 썸네일 변환 완료: outputSize={}", webpBytes.length);
		
		return webpBytes;
	}
	
}
