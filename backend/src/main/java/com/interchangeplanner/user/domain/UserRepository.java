package com.interchangeplanner.user.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);
}
