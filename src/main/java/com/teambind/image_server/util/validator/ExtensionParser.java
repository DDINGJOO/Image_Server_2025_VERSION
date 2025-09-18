package com.teambind.image_server.util.validator;


import org.springframework.stereotype.Component;

@Component
public class ExtensionParser {


    // 이미지의 확장자 파서
    public String extensionParse(String extension) {
        return extension.substring(extension.lastIndexOf(".") + 1).toUpperCase();
    }

}
