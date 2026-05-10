package com.king.paysim.domain.wallet;

import com.king.paysim.common.response.Response;
import com.king.paysim.common.util.AuthUtil;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.wallet.entity.Wallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wallets", description = "Wallet endpoints")
@RestController
@RequestMapping("/wallets")
@SecurityRequirement(name = "Bearer Auth")
public class WalletController {
    private final WalletService walletService;
    private final AuthUtil authUtil;

    public WalletController(WalletService walletService, AuthUtil authUtil) {
        this.walletService = walletService;
        this.authUtil = authUtil;
    }

    @Operation(summary = "Find all user wallets")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallets Retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Response<Wallet>> all() {
        User user = this.authUtil.getAuthUser();

        Wallet wallets = this.walletService.find(user.getId());
        return new ResponseEntity<>(
                Response.success(
                        "Wallets retrieved",
                        wallets
                        ),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Update user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet retrieved"),
            @ApiResponse(responseCode = "404", description = "Wallet not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<Wallet>> findById(@Parameter(description = "wallet id", schema = @Schema(type = "string")) @PathVariable String id) {

        Wallet wallet = this.walletService.findById(id);
        return new ResponseEntity<>(
                Response.success(
                        "Wallets retrieved",
                        wallet
                ),
                HttpStatus.OK
        );
    }

}
