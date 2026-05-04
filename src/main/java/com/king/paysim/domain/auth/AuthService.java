package com.king.paysim.domain.auth;

import com.king.paysim.common.responses.Response;
import com.king.paysim.common.utils.JwtUtil;
import com.king.paysim.core.kafka.MessageProducer;
import com.king.paysim.domain.auth.dtos.LoginRequestDto;
import com.king.paysim.domain.auth.dtos.LoginResponseDto;
import com.king.paysim.domain.auth.dtos.RegisterRequestDto;
import com.king.paysim.domain.auth.dtos.RegisterResponseDto;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.dtos.UserResponseDto;
import com.king.paysim.domain.user.entitities.User;
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
    public RegisterResponseDto register(RegisterRequestDto payload) {
        if (userRepository.findByEmail(payload.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User();
        user.setFirstName(payload.firstName());
        user.setLastName(payload.lastName());
        user.setEmail(payload.email());
        user.setPassword(passwordEncoder.encode(payload.password()));

        User savedUser = userRepository.save(user);

        messageProducer.sendMessage("wallet.create", String.valueOf(savedUser.getId()));

        UserResponseDto userDto = new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getBvn(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
        return new RegisterResponseDto(userDto);
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

        String token = jwt.generateToken(user.getEmail());

        UserResponseDto userDto = new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBvn(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        LoginResponseDto data = new LoginResponseDto(userDto, token);
        return Response.success("Login successful", data).getData();
    }


}
