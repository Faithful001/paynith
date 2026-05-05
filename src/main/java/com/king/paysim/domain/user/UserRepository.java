package com.king.paysim.domain.user;

import com.king.paysim.domain.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail (String email);
}
