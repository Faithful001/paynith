package com.king.paysim.domain.user;

import com.king.paysim.domain.user.dtos.UpdateUserDto;
import com.king.paysim.domain.user.entitities.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User getUserById (Long id) {
        return this.repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with this id does not exist"
                ));
    }

    @Transactional
    public User updateUser(Long id, UpdateUserDto payload) {

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
}
