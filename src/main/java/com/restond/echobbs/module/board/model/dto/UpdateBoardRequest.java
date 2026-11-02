package com.restond.echobbs.module.board.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBoardRequest(
        @NotBlank(message = "板块名称不能为空")
        @Size(max = 64, message = "板块名称不能超过64个字符")
        String name,

        @NotBlank(message = "板块标识不能为空")
        @Size(max = 64, message = "板块标识不能超过64个字符")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "板块标识只能包含小写字母、数字和连字符")
        String slug,

        @Size(max = 512, message = "板块描述不能超过512个字符")
        String description,

        Integer sortOrder
) {}