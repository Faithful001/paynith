package com.king.paysim.domain.wallet;

import com.king.paysim.domain.wallet.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByDedicatedAccId(Long dedicatedAccId);
}
