package com.restond.echobbs.module.comment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论内容不能超过2000个字符")
        String content
) {}
