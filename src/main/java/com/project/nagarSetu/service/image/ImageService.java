package com.project.nagarSetu.service.image;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

public interface ImageService {
    Map<String , String> saveImage(MultipartFile file , UUID id);
    String getImage(UUID id);

}
