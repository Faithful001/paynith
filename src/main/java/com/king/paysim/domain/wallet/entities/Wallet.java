package com.king.paysim.domain.wallet.entities;

import com.king.paysim.domain.user.entitities.User;
import com.king.paysim.domain.wallet.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "wallets",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"),
        indexes = {
            @Index(name = "idx_wallet_dedicated_acc_id", columnList = "dedicated_acc_id")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, precision = 19, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal balance;

    @Column(nullable = true)
    private Long dedicatedAccId;

    @Column(nullable = true)
    private String accountNumber;

    @Column(nullable = true)
    private String accountName;

    @Column(nullable = true)
    private String bankName;

    @Enumerated(EnumType.STRING)
    private WalletStatus status;

    @Column(nullable = true)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column()
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}


