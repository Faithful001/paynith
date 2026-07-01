package com.king.paysim.domain.card.entity;

import com.king.paysim.domain.card.enums.CardStatus;
import com.king.paysim.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column
    private String cardToken;           // flw-t1nf-xxx

    @Column
    private String last4;

    @Column
    private String expiry;              // MM/YY or full

    @Column
    private String brand;               // VISA, MASTERCARD, VERVE

    @Column
    private String cardType;            // DEBIT, CREDIT

    @Column
    private String authorizationMode;   // PIN, 3DS, etc.

    @Column
    @Enumerated(EnumType.STRING)
    private CardStatus status;    // ACTIVE, INACTIVE, EXPIRED, FAILED

    @Column
    private Boolean isDefault = false;

    @Column(updatable = false)
    private LocalDateTime linkedAt;

    @Column
    private LocalDateTime lastUsed;

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    @PrePersist
    protected void onCreate() {
        this.linkedAt = LocalDateTime.now();
    }
}