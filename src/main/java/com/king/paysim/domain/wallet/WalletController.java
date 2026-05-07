package com.king.paysim.domain.wallet;

import com.king.paysim.common.responses.Response;
import com.king.paysim.domain.user.UserService;
import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.wallet.entities.Wallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Wallets", description = "Wallet endpoints")
@RestController
@RequestMapping("/wallets")
@SecurityRequirement(name = "Bearer Auth")
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @Operation(summary = "Update user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallets Retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Response<List<Wallet>>> all() {
        User user = this.userService.getAuthUser();

        List<Wallet> wallets = this.walletService.findAll(user.getId());
        return new ResponseEntity<>(
                Response.success(
                        "Wallets retrieved",
                        wallets
                        ),
                HttpStatus.OK
        );
    }

}
