package com.restond.echobbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Spring Security 安全配置。
 * <p>
 * 核心安全策略配置：
 * <ul>
 *   <li>CORS：启用跨域支持（配合 {@link CorsConfig}）</li>
 *   <li>CSRF：页面路由启用，API 路径禁用（前后端分离使用 Session）</li>
 *   <li>会话管理：IF_REQUIRED 策略 + 会话固定攻击防护（migrateSession）</li>
 *   <li>权限规则：公开读接口 / 认证写接口 / ADMIN+MODERATOR 管理接口</li>
 *   <li>安全响应头：X-Frame-Options、XSS Protection、HSTS、Cache-Control</li>
 *   <li>Remember-Me：30 天有效期</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");
        http
                // 启用 CORS（配合 CorsConfig）
                .cors(cors -> {})
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(request -> request.getRequestURI().startsWith("/api/"))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // 会话固定攻击防护：登录后迁移会话 ID
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/register", "/api/login", "/api/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search", "/api/notifications/unread-count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/latest",
                                "/api/posts/hot", "/api/posts/recommended",
                                "/api/posts/*", "/api/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards").permitAll()
                        // /me 探测登录态：游客返回 null，避免 401 报错；写操作仍需登录
                        .requestMatchers(HttpMethod.GET, "/api/users/me").permitAll()
                        .requestMatchers("/api/posts/**", "/api/comments/**",
                                "/api/users/me/**", "/api/upload/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                        .anyRequest().permitAll()
                )
                .rememberMe(remember -> remember
                        .key("echobbs-remember-me-key")
                        .tokenValiditySeconds(30 * 24 * 3600)
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")
                        )
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .permitAll()
                )
                // 安全响应头（SecurityHeadersFilter 已自定义，此处补充 Spring Security 内置的）
                .headers(headers -> headers
                        // 点击劫持防护
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // XSS 保护
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // 不缓存受保护资源
                        .cacheControl(cache -> {})
                        // HSTS
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
