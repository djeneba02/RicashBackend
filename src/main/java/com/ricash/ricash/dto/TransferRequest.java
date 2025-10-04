package com.ricash.ricash.dto;

import com.ricash.ricash.model.Enum.methodePaiement;
import com.ricash.ricash.model.Enum.typeTransaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransferRequest {
    private String devise;
    private typeTransaction type;
    private methodePaiement methodePaiement;
    private double montant;
    private double montantTotal;

    // Gérer différentes casse avec @JsonProperty
    @JsonProperty("expediteurId")
    private Long expediteurId;

    @JsonProperty("destinataireId")
    private Long destinataireId;

    @JsonProperty("agentId")
    private Long agentId;

    @JsonProperty("agentExpediteurId")
    private Long agentExpediteurId;

    @JsonProperty("agentDestinataireId")
    private Long agentDestinataireId;

    private Long beneficiaireId;
}