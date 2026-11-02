package com.restond.echobbs.module.upload.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传服务。
 * <p>
 * 处理帖子内容中的图片上传，包含安全校验：
 * <ul>
 *   <li>文件非空检查</li>
 *   <li>文件大小限制（5MB）</li>
 *   <li>MIME 类型白名单（jpg/png/gif/webp）</li>
 *   <li>图片有效性验证（通过 ImageIO 解码）</li>
 *   <li>路径穿越防护（规范化路径后校验是否在上传目录内）</li>
 * </ul>
 * 上传后通过 thumbnailator 压缩至 1200x1200，使用 UUID 重命名防止文件名冲突。
 */
@Service
public class UploadService {
    @Value("${app.upload.image-dir:uploads/images}")
    private String imageDir;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大 5MB）");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("只支持 jpg、jpeg、png、gif、webp 格式");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new IllegalArgumentException("文件不是有效的图片");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取图片文件");
        }

        Path uploadDir = Paths.get(imageDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadDir.resolve(filename).normalize();

        if (!filePath.startsWith(uploadDir)) {
            throw new IllegalArgumentException("非法的文件路径");
        }

        Thumbnails.of(new ByteArrayInputStream(file.getBytes()))
                .size(1200, 1200)
                .outputQuality(0.85)
                .toFile(filePath.toFile());

        return "/uploads/images/" + filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
