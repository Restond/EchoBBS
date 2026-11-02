package com.restond.echobbs.module.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 50, message = "昵称长度1-50字符")
        String nickname,

        @Size(max = 500, message = "简介最多500字符")
        String bio,

        @Email(message = "邮箱格式不正确")
        String email
) {}
