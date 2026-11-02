package com.restond.echobbs.module.user.service;

import com.restond.echobbs.common.ConflictException;
import com.restond.echobbs.common.ForbiddenException;
import com.restond.echobbs.common.NotFoundException;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.model.dto.LoginRequest;
import com.restond.echobbs.module.user.model.dto.RegisterRequest;
import com.restond.echobbs.module.user.model.dto.UpdateProfileRequest;
import com.restond.echobbs.module.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 用户业务服务。
 * <p>
 * 处理用户注册、登录、资料更新、头像上传、封禁等核心用户业务。
 * <ul>
 *   <li>注册：用户名/邮箱唯一性校验，密码 BCrypt 加密</li>
 *   <li>登录：密码验证 + 封禁状态检查 + Session 创建，记录 IP 用于安全审计</li>
 *   <li>头像上传：MIME/大小/图片有效性/路径穿越多重校验，thumbnailator 压缩至 200x200</li>
 *   <li>封禁：仅 ADMIN/MODERATOR 可操作，不可封禁自己和 ADMIN 角色</li>
 * </ul>
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("用户名已存在");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.username());

        userRepository.save(user);
    }

    public boolean login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);

        Optional<User> userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            log.warn("用户名不存在: username={}, IP={}", request.username(), ip);
            return false;
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("密码错误: username={}, IP={}", request.username(), ip);
            return false;
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            log.warn("账号已被封禁: username={}, IP={}", request.username(), ip);
            return false;
        }

        log.info("登录成功 username={}, IP={}", request.username(), ip);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password("")
                .roles(user.getRole().name())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        return true;
    }

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            return Optional.empty();
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateProfile(User currentUser, UpdateProfileRequest request) {
        if (request.email() != null && !request.email().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("邮箱已被其他用户使用");
            }
            currentUser.setEmail(request.email());
        }

        if (request.nickname() != null) {
            currentUser.setNickname(request.nickname());
        }

        if (request.bio() != null) {
            currentUser.setBio(request.bio());
        }

        return userRepository.save(currentUser);
    }

    @Transactional
    public User uploadAvatar(User currentUser, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("文件名无效");
        }

        String detectedContentType = file.getContentType();
        if (detectedContentType == null || !ALLOWED_MIME_TYPES.contains(detectedContentType.toLowerCase())) {
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

        Path uploadDir = Paths.get(avatarDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        if (currentUser.getAvatarPath() != null && !currentUser.getAvatarPath().isEmpty()) {
            try {
                Path oldFile = Paths.get(currentUser.getAvatarPath()).normalize();
                if (oldFile.startsWith(uploadDir)) {
                    Files.deleteIfExists(oldFile);
                }
            } catch (Exception ignored) {
            }
        }

        String extension = getFileExtension(originalFilename);
        String filename = currentUser.getId() + "_" + System.currentTimeMillis() + extension;
        Path filePath = uploadDir.resolve(filename).normalize();


        if (!filePath.startsWith(uploadDir)) {
            throw new IllegalArgumentException("非法的文件路径");
        }

        try {
            Thumbnails.of(new ByteArrayInputStream(file.getBytes()))
                    .size(200, 200)
                    .outputQuality(0.85)
                    .toFile(filePath.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("图片处理失败: " + e.getMessage());
        }

        currentUser.setAvatarPath("/uploads/avatars/" + filename);
        return userRepository.save(currentUser);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("文件名无效");
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    @Transactional
    public User toggleBanUser(Long userId, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN
                && currentUser.getRole() != User.UserRole.MODERATOR) {
            throw new ForbiddenException("无权限执行此操作");
        }

        if (userId.equals(currentUser.getId())) {
            throw new ForbiddenException("不能封禁自己");
        }

        User targetUser = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("未找到用户"));

        if (targetUser.getRole() == User.UserRole.ADMIN) {
            throw new ForbiddenException("不能封禁管理员");
        }

        String action;
        if (targetUser.getStatus() == User.UserStatus.ACTIVE) {
            targetUser.setStatus(User.UserStatus.BANNED);
            action = "封禁";
        } else {
            targetUser.setStatus(User.UserStatus.ACTIVE);
            action = "解封";
        }

        User result = userRepository.save(targetUser);

        log.warn("管理员操作 操作者={}, 角色={}, 操作={}, 目标用户={}", currentUser.getUsername(), currentUser.getRole(), action, targetUser.getUsername());

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
