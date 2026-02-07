package com.king.paysim.domain.auth;

import com.king.paysim.domain.auth.dtos.LoginRequestDto;
import com.king.paysim.domain.auth.dtos.LoginResponseDto;
import com.king.paysim.domain.auth.dtos.RegisterRequestDto;
import com.king.paysim.domain.auth.dtos.RegisterResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public RegisterResponseDto Register (@Valid @RequestBody RegisterRequestDto payload){
        return authService.register(payload);
    }

    @PostMapping("/login")
    public LoginResponseDto Login (@Valid @RequestBody LoginRequestDto payload){
        return authService.login(payload);
    }
}
