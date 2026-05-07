package com.king.paysim.domain.user;

import com.king.paysim.domain.user.dtos.UpdateUserDto;
import com.king.paysim.domain.user.entities.User;
import io.jsonwebtoken.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User getUserById (String id) {
        return this.repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with this id does not exist"
                ));
    }

    @Transactional
    public User updateUser(String id, UpdateUserDto payload) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User with this id does not exist"
                        )
                );

        if (payload.firstName() != null && !payload.firstName().isBlank()) {
            user.setFirstName(payload.firstName());
        }

        if (payload.lastName() != null && !payload.lastName().isBlank()) {
            user.setLastName(payload.lastName());
        }

        if (payload.phoneNumber() != null && !payload.phoneNumber().isBlank()) {
            user.setPhoneNumber(payload.phoneNumber());
        }

        if (payload.bvn() != null && !payload.bvn().isBlank()) {
            if (user.getBvn() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "BVN cannot be updated once set"
                );
            }
            user.setBvn(payload.bvn());
        }

        this.repository.save(user);

        return user;
    }

    public User getAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authenticated");
        }

        String userId = (String) authentication.getPrincipal();

        return this.getUserById(userId);
    }
}
