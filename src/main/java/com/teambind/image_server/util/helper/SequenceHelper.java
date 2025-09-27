package com.teambind.image_server.util.helper;


import com.teambind.image_server.repository.ImageRepository;
import com.teambind.image_server.repository.ImageSequenceRepository;
import org.springframework.stereotype.Component;

@Component
public class SequenceHelper {

    private ImageSequenceRepository imageSequenceRepository;
    private ImageRepository imageRepository;


}
