package com.teambind.image_server.event.events;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SequentialImageChangeEvent {
    private String imageId;
    private String imageUrl;
    private String referenceId;
}
