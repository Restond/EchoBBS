package com.restond.echobbs.module.upload.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.upload.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * 图片上传 API 控制器。
 * <p>
 * 提供帖子内容中的图片上传接口，需登录。
 * 文件安全校验在 {@link UploadService} 中实现。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadApiController {
    private final UploadService uploadService;

    public UploadApiController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = uploadService.uploadImage(file);
        return Result.ok(imageUrl);
    }
}

