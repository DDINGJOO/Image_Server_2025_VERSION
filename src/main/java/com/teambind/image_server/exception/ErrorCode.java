package com.teambind.image_server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {
    INVALID_REFERENCE("INVALID_REFERENCE", "Invalid Reference", HttpStatus.BAD_REQUEST),
    INVALID_EXTENSION("INVALID_EXTENSION", "Invalid Extension", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_VARIANT("INVALID_IMAGE_VARIANT", "Invalid Image Variant", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_STATUS("INVALID_IMAGE_STATUS", "Invalid Image Status", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_ID("INVALID_IMAGE_ID", "Invalid Image Id", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_SEQUENCE("INVALID_IMAGE_SEQUENCE", "Invalid Image Sequence", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "Image Not Found", HttpStatus.NOT_FOUND),
    IMAGE_VARIANT_NOT_FOUND("IMAGE_VARIANT_NOT_FOUND", "Image Variant Not Found", HttpStatus.NOT_FOUND),
    IMAGE_SEQUENCE_NOT_FOUND("IMAGE_SEQUENCE_NOT_FOUND", "Image Sequence Not Found", HttpStatus.NOT_FOUND),
    IMAGE_VARIANT_ALREADY_EXISTS("IMAGE_VARIANT_ALREADY_EXISTS", "Image Variant Already Exists", HttpStatus.BAD_REQUEST),
    IMAGE_SEQUENCE_ALREADY_EXISTS("IMAGE_SEQUENCE_ALREADY_EXISTS", "Image Sequence Already Exists", HttpStatus.BAD_REQUEST),
    IMAGE_VARIANT_NOT_AVAILABLE("IMAGE_VARIANT_NOT_AVAILABLE", "Image Variant Not Available", HttpStatus.BAD_REQUEST),
    IMAGE_SEQUENCE_NOT_AVAILABLE("IMAGE_SEQUENCE_NOT_AVAILABLE", "Image Sequence Not Available", HttpStatus.BAD_REQUEST),
    IMAGE_VARIANT_NOT_AVAILABLE_TO_USER("IMAGE_VARIANT_NOT_AVAILABLE_TO_USER", "Image Variant Not Available To User", HttpStatus.BAD_REQUEST),


    IOException("IOException", "IO Exception Occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    REFERENCE_TYPE_NOT_FOUND("REFERENCE_TYPE_NOT_FOUND", "Reference Type Not Found", HttpStatus.BAD_REQUEST),
    FILE_EXTENSION_NOT_FOUND("FILE_EXTENSION_NOT_FOUND", "File Extension Not Found", HttpStatus.BAD_REQUEST),
    IMAGE_SAVE_FAILED("IMAGE_SAVE_FAILED", "Image Save Failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ;


    private final String errCode;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String errCode, String message, HttpStatus status) {

        this.status = status;
        this.errCode = errCode;
        this.message = message;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                " status='" + status + '\'' +
                "errCode='" + errCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
