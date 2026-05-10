package com.king.paysim.domain.auth;

import com.king.paysim.common.response.Response;
import com.king.paysim.domain.auth.dto.LoginRequestDto;
import com.king.paysim.domain.auth.dto.LoginResponseDto;
import com.king.paysim.domain.auth.dto.RegisterRequestDto;
import com.king.paysim.domain.auth.dto.RegisterResponseDto;
import com.king.paysim.domain.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth Controller", description = "Authentication endpoints")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(summary = "Register a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<Response<RegisterResponseDto>> Register (@Valid @RequestBody RegisterRequestDto payload){
        UserResponseDto user = authService.register(payload);

        RegisterResponseDto registerResponseDto = new RegisterResponseDto(user);

        return new ResponseEntity<>(Response.success("User registered", registerResponseDto), HttpStatus.CREATED);
    }

    @Operation(summary = "User login")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<Response<LoginResponseDto>> Login (@Valid @RequestBody LoginRequestDto payload){
        LoginResponseDto result = authService.login(payload);
        return new ResponseEntity<>(Response.success("Login successful", result), HttpStatus.OK);
    }
}
