package com.king.paysim.domain.auth;

import com.king.paysim.common.utils.JwtUtil;
import com.king.paysim.core.kafka.MessageProducer;
import com.king.paysim.domain.auth.dtos.LoginRequestDto;
import com.king.paysim.domain.auth.dtos.LoginResponseDto;
import com.king.paysim.domain.auth.dtos.RegisterRequestDto;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.dtos.UserResponseDto;
import com.king.paysim.domain.user.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwt;
    private final MessageProducer messageProducer;

    public AuthService (
            UserRepository userRepository,
            JwtUtil jwt,
            MessageProducer messageProducer
    ) {
        this.userRepository = userRepository;
        this.jwt = jwt;
        this.messageProducer = messageProducer;
    }

    @Transactional
    public UserResponseDto register(RegisterRequestDto payload) {
        if (userRepository.findByEmail(payload.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User();
        user.setFirstName(payload.firstName());
        user.setLastName(payload.lastName());
        user.setEmail(payload.email());
        user.setPhoneNumber(payload.phoneNumber());
        user.setPassword(passwordEncoder.encode(payload.password()));

        User savedUser = userRepository.save(user);

        messageProducer.sendMessage("wallet.create", savedUser.getId());

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedUser.getBvn(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    public LoginResponseDto login (LoginRequestDto payload) {
        Optional<User> userEntity = userRepository.findByEmail(payload.email());

        if (userEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or password");
        }

        User user = userEntity.get();
        boolean passwordsMatch = passwordEncoder.matches(payload.password(), user.getPassword());

        if (!passwordsMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or password");
        }

        String token = jwt.generateToken(user.getId());

        UserResponseDto userResponse = new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBvn(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return new LoginResponseDto(userResponse, token);

    }


}
