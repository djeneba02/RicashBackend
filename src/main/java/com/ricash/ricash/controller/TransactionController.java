package com.ricash.ricash.controller;

import com.ricash.ricash.dto.TransferRequest;
import com.ricash.ricash.model.Transaction;
import com.ricash.ricash.service.transactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final transactionService transactionService;

    @PostMapping("/initier")
    public ResponseEntity<Transaction> initierTransfert(@RequestBody TransferRequest request) {
        Transaction transaction = transactionService.initierTransfert(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/executer/{transactionId}")
    public ResponseEntity<Transaction> executerTransfert(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.executerTransfert(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        // Implémentez la récupération de transaction
        return ResponseEntity.ok().build();
    }
}