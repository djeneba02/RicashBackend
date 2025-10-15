package com.ricash.ricash.service;

import com.ricash.ricash.dto.TransferRequest;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.model.Transaction;
import java.util.List;

public interface transactionService {
    Transaction initierTransfert(TransferRequest request, String token);
    Transaction executerTransfert(Long transactionId);
    Transaction getTransactionById(Long id);
    Transaction getTransactionByCode(String codeTransaction);
    List<Transaction> getTransactionsByStatut(statutTransaction statut);
    List<Transaction> getTransactionsByUserId(Long userId);
    List<Transaction> getTransactionsByAgentId(Long agentId);
    Transaction annulerTransaction(Long transactionId);
    List<Transaction> getAllTransactions();
    Transaction rejeterTransaction(Long transactionId, String raison);
}