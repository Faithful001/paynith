package com.king.paysim.domain.transaction;

import com.king.paysim.domain.transaction.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

}
