package com.ricash.ricash.dto;

import com.google.firebase.database.annotations.NotNull;
import com.ricash.ricash.model.Enum.methodePaiement;
import com.ricash.ricash.model.Enum.typeTransaction;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String devise;
    private typeTransaction type;
    private methodePaiement methodePaiement;
    private double montantTotal;
    private double montant;

    // Selon le type de transaction
    private Long expediteurId;
    private Long destinataireId;
    private Long agentId;
    private Long agentExpediteurId;
    private Long agentDestinataireId;
    private Long beneficiaireId;
}

