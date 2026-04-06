package com.tablesoccer.ranker.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByDisplayNameIgnoreCase(String displayName);

    List<User> findByActiveTrue();

    long countByRole(Role role);
}
