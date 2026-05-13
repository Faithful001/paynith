package com.king.paysim.domain.wallet;

import com.king.paysim.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    Optional<Wallet> findByUserId(String userId);
    Optional<Wallet> findByUserEmail(String email);
    Optional<Wallet> findByAccountNumber(String accountNumber);
    Optional<Wallet> findByIdAndUserId(String walletId, String userId);
    List<Wallet> findAllByUserId(String userId);
    List<Wallet> findAllByUserEmail(String email);
}
