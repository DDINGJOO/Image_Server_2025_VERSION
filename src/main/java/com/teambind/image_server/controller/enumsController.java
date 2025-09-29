package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.teambind.image_server.ImageServerApplication.extensionMap;
import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@RestController
@RequestMapping("/api/enums")
public class enumsController {
    @GetMapping("/extensions")
    public Map<String, Extension> getEnums() {
        return extensionMap;
    }

    @GetMapping("/referenceType")
    public Map<String, ReferenceType> getReferenceTypes() {
        return referenceTypeMap;
    }
}
