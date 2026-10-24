package com.edu.platform.service;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.entity.User;
import com.edu.platform.domain.enums.Role;
import com.edu.platform.dto.AuthDtos;
import com.edu.platform.repository.UserRepository;
import com.edu.platform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the account lifecycle rules: public sign-up, duplicate
 * usernames, disabled accounts and password changes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService - registration, login state, password change")
class AuthServiceTest {

    private static final long USER_ID = 7L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    private User activeStudent() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setPassword("$2a$stored-hash");
        user.setRole(Role.STUDENT);
        user.setStatus(1);
        return user;
    }

    @Test
    @DisplayName("sign-up always stores a bcrypt hash (never the raw password) and creates a STUDENT")
    void registerStoresHashedPasswordAndStudentRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("$2a$encoded-hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthDtos.UserVO vo = service.register(new AuthDtos.RegisterRequest(
                "alice", "Passw0rd!", "Alice Chen", "alice@example.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("$2a$encoded-hash"); // not the raw password
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);           // public sign-up is never ADMIN
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(vo.username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("a taken username is rejected before anything is saved")
    void duplicateUsernameIsRejected() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new AuthDtos.RegisterRequest(
                " alice ", "Passw0rd!", null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("already taken");
        verify(userRepository, never()).save(any()); // nothing may be written
    }

    @Test
    @DisplayName("a disabled account cannot sign in even with the correct password")
    void disabledAccountCannotLogin() {
        User disabled = activeStudent();
        disabled.setStatus(0);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(disabled));
        when(passwordEncoder.matches("Passw0rd!", "$2a$stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new AuthDtos.LoginRequest("alice", "Passw0rd!")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("password change requires the correct current password")
    void passwordChangeVerifiesCurrentPassword() {
        User user = activeStudent();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-old", "$2a$stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(USER_ID,
                new AuthDtos.PasswordChangeRequest("wrong-old", "NewPass123")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Current password is incorrect");

        when(passwordEncoder.matches("Passw0rd!", "$2a$stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$new-hash");
        service.changePassword(USER_ID, new AuthDtos.PasswordChangeRequest("Passw0rd!", "NewPass123"));

        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("$2a$new-hash");
    }
}
