package com.project.nagarSetu.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

@Slf4j
@AllArgsConstructor
@Service
public class ImageServiceImplementation implements ImageService {

    private final Cloudinary cloudinary;
    private final String issueImg = "ISSUE_IMG_";

    @Override
    public Map<String, String> saveImage(MultipartFile file, UUID id) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        Map<String, Object> params = ObjectUtils.asMap(
                "public_id", issueImg + id,
                "overwrite", true,
                "resource_type", "image"
        );

        try {
            byte[] imageBytes = file.getBytes();
            try {
                BufferedImage originalImage = ImageIO.read(file.getInputStream());
                if (originalImage != null) {
                    int maxWidth = 1920;
                    int maxHeight = 1080;
                    if (originalImage.getWidth() > maxWidth || originalImage.getHeight() > maxHeight) {
                        double widthRatio = (double) maxWidth / originalImage.getWidth();
                        double heightRatio = (double) maxHeight / originalImage.getHeight();
                        double ratio = Math.min(widthRatio, heightRatio);
                        int newWidth = (int) (originalImage.getWidth() * ratio);
                        int newHeight = (int) (originalImage.getHeight() * ratio);
                        
                        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = resizedImage.createGraphics();
                        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                        g.dispose();
                        
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(resizedImage, "jpg", baos);
                        imageBytes = baos.toByteArray();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to compress image, uploading original bytes", e);
            }

            Map uploadResult =
                    cloudinary.uploader().upload(imageBytes, params);

            return uploadResult;

        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    @Override
    public String getImage(UUID id) {
        return cloudinary.url().generate(issueImg + id);

    }
}
