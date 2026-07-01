package com.king.paynith.common.util;

import com.king.paynith.domain.user.UserRepository;
import com.king.paynith.domain.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthUtil {
    private final UserRepository userRepository;

    public AuthUtil(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User getAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authenticated");
        }

        String userId = (String) authentication.getPrincipal();

        return this.userRepository.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authenticated"));
    }

    public String getAuthUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authenticated");
        }

        return (String) authentication.getPrincipal();
    }
}