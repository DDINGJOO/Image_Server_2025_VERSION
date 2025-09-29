package com.teambind.image_server.controller;


import com.teambind.image_server.entity.Extension;
import com.teambind.image_server.entity.ReferenceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.teambind.image_server.ImageServerApplication.extensionMap;
import static com.teambind.image_server.ImageServerApplication.referenceTypeMap;

@RestController
@RequestMapping("/api/enums")
public class enumsController {
    @GetMapping("/extensions")

    public ResponseEntity<Map<String, String>> getEnums() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Extension> entry : extensionMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return ResponseEntity.ok(map);
    }

    @GetMapping("/referenceType")
    public ResponseEntity<Map<String, String>> getReferenceTypes() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, ReferenceType> entry : referenceTypeMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return ResponseEntity.ok(map);
    }
}
