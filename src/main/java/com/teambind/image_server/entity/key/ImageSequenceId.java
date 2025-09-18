package com.teambind.image_server.entity.key;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class ImageSequenceId implements Serializable {
    private String imageId;
    private Long id;
}
