package com.king.paysim.domain.wallet;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entitities.User;
import com.king.paysim.domain.wallet.dtos.CreateVAResponse;
import com.king.paysim.domain.wallet.dtos.CreateWalletDto;
import com.king.paysim.domain.wallet.entities.Wallet;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Value("${PAYSTACK_SEC_KEY}")
    private String payStackSecKey;

    public WalletService(WalletRepository walletRepository,  UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet create(CreateWalletDto payload) {
        User user = userRepository.findById(payload.userId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (this.walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already exists");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.PENDING);
        wallet.setBalance(BigDecimal.ZERO);

        wallet = walletRepository.save(wallet);

        try {
            this.createPaystackVA(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber()
            );
        } catch(Exception e) {
            wallet.setStatus(WalletStatus.FAILED);
            wallet.setFailureReason(e.getMessage());
        }

        wallet = walletRepository.save(wallet);

        return wallet;
    }

    public Wallet get(Long userId){
        return walletRepository.findByUserId(userId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "This user does not have a wallet yet"));
    }

    private CreateVAResponse createPaystackVA(String email, String firstName, String lastName, String phone){
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + payStackSecKey)
                .defaultHeader("Content-Type", "application/json")
                .build();


        return webClient.post()
                .uri("dedicated_account/assign")
                .bodyValue(Map.of(
                        "email", email,
                        "first_name", firstName,
                        "last_name", lastName,
                        "phone", phone,
                        "preferred_bank", "wema-bank",
                        "country", "NG"
                ))
                .retrieve()
                .bodyToMono(CreateVAResponse.class)
                .block();
    }
}
