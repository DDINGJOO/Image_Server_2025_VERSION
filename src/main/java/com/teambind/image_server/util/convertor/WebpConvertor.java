package com.teambind.image_server.util.convertor;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import com.teambind.image_server.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class WebpConvertor {

    @Value("${image.dir}")
    private String PHOTO_DIR;

    public File convertToWebp(String fileName, File originalFile) {
        try {
            return ImmutableImage.loader()// 라이브러리 객체 생성
                    .fromFile(originalFile) // .jpg or .png File 가져옴
                    .output(WebpWriter.DEFAULT, new File(PHOTO_DIR + fileName + ".webp")); // 손실 압축 설정, fileName.webp로 파일 생성
        } catch (Exception e) {
            //TODO
            throw new CustomException(e.getMessage());
        }
    }


    public File convertToWebpWithLossless(String fileName, File originalFile) {
        try {
            return ImmutableImage.loader()// 라이브러리 객체 생성
                    .fromFile(originalFile) // .jpg or .png File 가져옴
                    .output(WebpWriter.DEFAULT.withLossless(), new File(PHOTO_DIR + fileName + ".webp")); // 무손실 압축 설정, fileName.webp로 파일 생성
        } catch (Exception e) {
            //TODO
            throw new CustomException(e.getMessage());
        }
    }
}
