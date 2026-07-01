package com.king.paynith.domain.user;

import com.king.paynith.common.response.Response;
import com.king.paynith.domain.user.dto.UpdateUserDto;
import com.king.paynith.domain.user.dto.UserResponseDto;
import com.king.paynith.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Auth")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<UserResponseDto>> getUser(
            @Parameter(
                    description = "User ID",
                    schema = @Schema(type = "string")
            )
            @PathVariable String id
    ) {
        User user = this.userService.getUserById(id);

        UserResponseDto response = new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBvn(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return new ResponseEntity<>(
                Response.success("User found", response),
                HttpStatus.OK);
    }

    @Operation(summary = "Update user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Response<UserResponseDto>> updateUser(
            @Parameter(
                    description = "User ID",
                    schema = @Schema(type = "string")
            )
            @PathVariable String id,
            @Valid @RequestBody UpdateUserDto payload
    ) {
        User user = this.userService.updateUser(id, payload);

        UserResponseDto response = new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBvn(),
                user.getPhoneNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return new ResponseEntity<>(
                Response.success("User updated", response),
                HttpStatus.OK
        );
    }
}