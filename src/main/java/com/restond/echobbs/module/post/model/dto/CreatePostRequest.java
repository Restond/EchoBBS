package com.restond.echobbs.module.post.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull(message = "板块ID不能为空")
        Long boardId,

        @NotBlank(message = "标题不能为空")
        @Size(min = 1, max = 256, message = "标题长度必须在1-256之间")
        String title,

        @NotBlank(message = "内容不能为空")
        @Size(min = 1, max = 50000, message = "内容长度必须在1-50000字符之间")
        String content
) {}
