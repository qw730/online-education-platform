package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.User;
import com.edu.platform.domain.enums.Role;
import com.edu.platform.dto.AuthDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.repository.UserRepository;
import com.edu.platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Registration, sign in and profile management. Public sign-up always creates a
 * STUDENT account; administrators are created by the demo data initialiser.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthDtos.UserVO register(AuthDtos.RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BizException("Username is already taken");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(StringUtils.hasText(request.realName()) ? request.realName().trim() : username);
        user.setEmail(request.email());
        user.setRole(Role.STUDENT);
        user.setStatus(1);
        return toVO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> BizException.unauthorized("Incorrect username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BizException.unauthorized("Incorrect username or password");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("This account has been disabled");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthDtos.LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), toVO(user));
    }

    @Transactional(readOnly = true)
    public User getEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserVO getProfile(Long userId) {
        return toVO(getEntity(userId));
    }

    @Transactional
    public AuthDtos.UserVO updateProfile(Long userId, AuthDtos.ProfileUpdateRequest request) {
        User user = getEntity(userId);
        if (StringUtils.hasText(request.realName())) {
            user.setRealName(request.realName().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        return toVO(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.PasswordChangeRequest request) {
        User user = getEntity(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PageResult<AuthDtos.UserVO> listUsers(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> result = StringUtils.hasText(keyword)
                ? userRepository.findByUsernameContainingIgnoreCaseOrRealNameContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), pageable)
                : userRepository.findAll(pageable);
        return PageResult.of(result, this::toVO);
    }

    @Transactional
    public AuthDtos.UserVO toggleUserStatus(Long userId) {
        User user = getEntity(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BizException("Administrator accounts cannot be disabled");
        }
        user.setStatus(user.getStatus() != null && user.getStatus() == 1 ? 0 : 1);
        return toVO(userRepository.save(user));
    }

    public AuthDtos.UserVO toVO(User user) {
        return new AuthDtos.UserVO(user.getId(), user.getUsername(), user.getRealName(),
                user.getEmail(), user.getRole(), user.getAvatar(), user.getBio(),
                user.getStatus(), user.getCreatedAt());
    }
}
