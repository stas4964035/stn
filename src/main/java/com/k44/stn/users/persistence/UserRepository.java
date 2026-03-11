package com.k44.stn.users.persistence;

import com.k44.stn.users.domain.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(@NotNull String email);
    boolean existsByEmail(@NotNull String email);
}
