package com.edu.platform.security;

import com.edu.platform.common.BizException;
import com.edu.platform.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests of the JWT issuer / verifier. No Spring context, no database:
 * a pure component test that runs in milliseconds.
 */
@DisplayName("JwtUtil - token issue & verify")
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long!!";
    private static final String ISSUER = "online-education-platform";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpireMinutes(60);
        properties.setIssuer(ISSUER);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    @DisplayName("generate + parse round trip keeps userId / username / role")
    void roundTrip() {
        String token = jwtUtil.generateToken(42L, "alice", Role.STUDENT);

        AuthenticatedUser user = jwtUtil.parse(token);

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken(1L, "alice", Role.STUDENT);

        JwtProperties otherSecret = new JwtProperties();
        otherSecret.setSecret("another-secret-key-that-is-definitely-32-bytes-long!!!");
        otherSecret.setExpireMinutes(60);
        otherSecret.setIssuer(ISSUER);

        assertThatThrownBy(() -> new JwtUtil(otherSecret).parse(token))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("an expired token is rejected as unauthorized")
    void expiredTokenIsRejected() {
        JwtProperties expired = new JwtProperties();
        expired.setSecret(SECRET);
        expired.setExpireMinutes(-5); // already expired
        expired.setIssuer(ISSUER);
        String token = new JwtUtil(expired).generateToken(1L, "alice", Role.ADMIN);

        assertThatThrownBy(() -> jwtUtil.parse(token))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("sign in");
    }

    @Test
    @DisplayName("a token issued by another system (wrong issuer) is rejected")
    void wrongIssuerIsRejected() {
        JwtProperties foreign = new JwtProperties();
        foreign.setSecret(SECRET);
        foreign.setExpireMinutes(60);
        foreign.setIssuer("some-other-system");
        String token = new JwtUtil(foreign).generateToken(1L, "alice", Role.STUDENT);

        assertThatThrownBy(() -> jwtUtil.parse(token))
                .isInstanceOf(BizException.class);
    }
}
