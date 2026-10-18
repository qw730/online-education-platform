package com.edu.platform.repository;

import com.edu.platform.domain.entity.User;
import com.edu.platform.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(Role role);

    Page<User> findByUsernameContainingIgnoreCaseOrRealNameContainingIgnoreCase(
            String username, String realName, Pageable pageable);

    List<User> findTop8ByRoleOrderByCreatedAtDesc(Role role);
}
