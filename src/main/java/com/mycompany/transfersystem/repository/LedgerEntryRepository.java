package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LedgerEntry;
import com.mycompany.transfersystem.entity.enums.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.transaction.id = :transactionId AND l.entryType = :entryType")
    BigDecimal sumByTransactionAndType(@Param("transactionId") Long transactionId, @Param("entryType") EntryType entryType);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.entryType = 'CREDIT' THEN l.amount ELSE -l.amount END), 0) FROM LedgerEntry l WHERE l.account.id = :accountId")
    BigDecimal calculateBalance(@Param("accountId") Long accountId);
}
