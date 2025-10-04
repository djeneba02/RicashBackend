package com.ricash.ricash.controller;

import com.ricash.ricash.dto.TransferRequest;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.model.Transaction;
import com.ricash.ricash.service.transactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final transactionService transactionService;

    @PostMapping("/initier")
    public Transaction initierTransfert(@RequestBody TransferRequest request,
                                        @RequestHeader("Authorization") String token) {
        String cleanToken = token.replace("Bearer ", "");
        return transactionService.initierTransfert(request, cleanToken);
    }

    @PostMapping("/executer/{transactionId}")
    public ResponseEntity<Transaction> executerTransfert(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.executerTransfert(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/code/{codeTransaction}")
    public ResponseEntity<Transaction> getTransactionByCode(@PathVariable String codeTransaction) {
        Transaction transaction = transactionService.getTransactionByCode(codeTransaction);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Transaction>> getTransactionsByStatut(@PathVariable statutTransaction statut) {
        List<Transaction> transactions = transactionService.getTransactionsByStatut(statut);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/utilisateur/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<Transaction>> getTransactionsByAgent(@PathVariable Long agentId) {
        List<Transaction> transactions = transactionService.getTransactionsByAgentId(agentId);
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/annuler/{transactionId}")
    public ResponseEntity<Transaction> annulerTransaction(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.annulerTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/rejeter/{transactionId}")
    public ResponseEntity<Transaction> rejeterTransaction(
            @PathVariable Long transactionId,
            @RequestParam String raison) {
        Transaction transaction = transactionService.rejeterTransaction(transactionId, raison);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/en-cours")
    public ResponseEntity<List<Transaction>> getTransactionsEnCours() {
        List<Transaction> transactions = transactionService.getTransactionsByStatut(statutTransaction.EN_COURS);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/completees")
    public ResponseEntity<List<Transaction>> getTransactionsCompletees() {
        List<Transaction> transactions = transactionService.getTransactionsByStatut(statutTransaction.COMPLETEE);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/rejetees")
    public ResponseEntity<List<Transaction>> getTransactionsRejetees() {
        List<Transaction> transactions = transactionService.getTransactionsByStatut(statutTransaction.REJETEE);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/annulees")
    public ResponseEntity<List<Transaction>> getTransactionsAnnulees() {
        List<Transaction> transactions = transactionService.getTransactionsByStatut(statutTransaction.ANNULEE);
        return ResponseEntity.ok(transactions);
    }
}