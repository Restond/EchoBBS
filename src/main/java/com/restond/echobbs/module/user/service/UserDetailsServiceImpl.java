package com.restond.echobbs.module.user.service;

import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Spring Security 用户详情服务实现。
 * <p>
 * 将数据库中的 {@link User} 实体转换为 Spring Security 的 {@link UserDetails}，
 * 用于认证和授权。封禁用户（BANNED）在此处被拦截，无法通过认证。
 * 角色映射为 {@code ROLE_} 前缀格式（如 {@code ROLE_ADMIN}）。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new UsernameNotFoundException("用户已被封禁: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
