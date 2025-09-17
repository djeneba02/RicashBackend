package com.ricash.ricash.service;


import com.ricash.ricash.dto.TransferRequest;
import com.ricash.ricash.model.Transaction;

public interface transactionService {
    Transaction initierTransfert(TransferRequest request);
    Transaction executerTransfert(Long transactionId);

}

