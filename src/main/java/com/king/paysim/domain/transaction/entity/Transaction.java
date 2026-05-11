package com.king.paysim.domain.transaction.entity;

import com.king.paysim.domain.transaction.enums.TransactionStatus;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.entity.User;
import com.king.paysim.domain.wallet.entity.Wallet;
import com.king.paysim.domain.wallet.enums.WalletCurrency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletCurrency currency = WalletCurrency.NGN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(unique = true)
    private String providerRef;

    @Column(unique = true, nullable = false)
    private String reference;

    @Column
    private String narration;

    @Column
    private String recipientAccountNumber;

    @Column
    private String recipientBankName;

    @Column
    private String recipientAccountName;

    @Column
    private String failureReason;

    @Column
    private BigDecimal fee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}