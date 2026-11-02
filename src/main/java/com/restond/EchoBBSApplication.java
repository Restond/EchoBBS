package com.restond;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EchoBBS 应用入口。
 * <p>
 * - {@code @EnableScheduling} - 启用定时任务（浏览量批量刷库）
 * - {@code @EnableSpringDataWebSupport} - 分页数据序列化为 DTO 模式，避免懒加载问题
 */
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class EchoBBSApplication {
    public static void main(String[] args) {
        SpringApplication.run(EchoBBSApplication.class);
    }
}