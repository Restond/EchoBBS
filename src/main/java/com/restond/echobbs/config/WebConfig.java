package com.restond.echobbs.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置。
 * <p>
 * 注册静态资源处理器（上传文件目录映射）和全局拦截器（限流、封禁用户拦截）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimitInterceptor;
    private final BannedUserInterceptor bannedUserInterceptor;

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;


    public WebConfig(BannedUserInterceptor bannedUserInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.bannedUserInterceptor = bannedUserInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadsRoot = Paths.get(avatarDir).getParent().toAbsolutePath().normalize();

        if (!uploadsRoot.toString().contains("uploads")) {
            throw new IllegalStateException("上传目录配置异常: " + uploadsRoot);
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsRoot+ "/")
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(bannedUserInterceptor).addPathPatterns("/**");
    }
}
