package com.edu.platform.domain.entity;

import com.edu.platform.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Platform account: either a STUDENT or an ADMIN.
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username")
})
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "bio", length = 255)
    private String bio;

    /** 1 = enabled, 0 = disabled. */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
