package com.ricash.ricash.repository;

import com.ricash.ricash.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface transactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByExpediteur_Uid(String expediteurUid);
    List<Transaction> findByDestinataire_Uid(String destinataireUid);
    List<Transaction> findByAgent_Id(Long agentId);
    Optional<Transaction> findByReference(String reference);
}
