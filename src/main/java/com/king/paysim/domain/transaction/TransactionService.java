package com.king.paysim.domain.transaction;

import com.king.paysim.domain.transaction.dtos.CreateTransactionDto;
import com.king.paysim.domain.transaction.entities.Transaction;
import com.king.paysim.domain.transaction.enums.TransactionStatus;
import com.king.paysim.domain.transaction.enums.TransactionType;
import com.king.paysim.domain.user.UserRepository;
import com.king.paysim.domain.user.entities.User;
import com.king.paysim.domain.wallet.WalletRepository;
import com.king.paysim.domain.wallet.entities.Wallet;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            WalletRepository walletRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    // ===================== CREATE =====================

    @Transactional
    public Transaction create(CreateTransactionDto payload, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findById(payload.walletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        Transaction transaction = Transaction.builder()
                .amount(payload.amount())
                .currency(payload.currency())
                .user(user)
                .wallet(wallet)
                .status(TransactionStatus.PENDING)
                .type(payload.transactionType())
                .providerRef(payload.providerRef().orElse(null))
                .reference(payload.reference().orElse(UUID.randomUUID().toString()))
                .narration(payload.narration().orElse(null))
                .recipientAccountNumber(payload.recipientAccountNumber().orElse(null))
                .recipientBankName(payload.recipientBankName().orElse(null))
                .recipientAccountName(payload.recipientAccountName().orElse(null))
                .fee(payload.fee())
                .build();

        return transactionRepository.save(transaction);
    }

    public boolean existsByReference(String reference) {
        return transactionRepository.existsByReference(reference);
    }

    // ===================== READ =====================

    public Transaction getById(String transactionId, String userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return transaction;
    }

    public Page<Transaction> findUserTransactions(
            String userId,
            TransactionType type,
            TransactionStatus status,
            String reference,
            String providerRef,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        // unique identifiers
        if (reference != null) {
            Transaction tx = getByReference(reference);

            if (!tx.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            return Page.empty(pageable).map(t -> tx);
        }

        if (providerRef != null) {
            Transaction tx = getByProviderRef(providerRef);

            if (!tx.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            return Page.empty(pageable).map(t -> tx);
        }

        // Filter combinations
        if (type != null && status != null) {
            return transactionRepository.findByUserIdAndTypeAndStatus(
                    userId, type, status, pageable
            );
        }

        if (type != null) {
            return transactionRepository.findByUserIdAndType(
                    userId, type, pageable
            );
        }

        if (status != null) {
            return transactionRepository.findByUserIdAndStatus(
                    userId, status, pageable
            );
        }

        // Default: all transactions
        return transactionRepository.findByUserId(userId, pageable);
    }

    public Page<Transaction> getAllByWalletId(String walletId, String userId, int page, int size) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return transactionRepository.findByWalletId(
                walletId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    public Transaction getByProviderRef(String providerRef) {
        return transactionRepository.findByProviderRef(providerRef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public Transaction getByReference(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    // ===================== UPDATE =====================

    @Transactional
    public Transaction markAsSuccessful(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction markAsFailed(String transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction markAsReversed(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transaction.setStatus(TransactionStatus.REVERSED);
        return transactionRepository.save(transaction);
    }

    // ===================== STATS =====================

    // get total amount credited to a wallet
    public BigDecimal getTotalCredited(String walletId) {
        return transactionRepository
                .sumAmountByWalletAndTypeAndStatus(
                        walletId,
                        TransactionType.CREDIT,
                        TransactionStatus.SUCCESSFUL
                );
    }

    // get total amount debited from a wallet
    public BigDecimal getTotalDebited(String walletId) {
        return transactionRepository
                .sumAmountByWalletAndTypeAndStatus(
                        walletId,
                        TransactionType.DEBIT,
                        TransactionStatus.SUCCESSFUL
                );
    }
}