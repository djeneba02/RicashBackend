package com.ricash.ricash.dto;

import com.ricash.ricash.model.Enum.methodePaiement;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.model.Enum.typeTransaction;
import lombok.Data;

import java.util.Date;

@Data
public class TransactionDTO {
    private Long id;
    private String reference;
    private double montant;
    private String devise;
    private double frais;
    private double montantTotal;
    private double tauxChange;
    private String codeTransaction;
    private String raisonRejet;
    private statutTransaction statut;
    private typeTransaction type;
    private methodePaiement methodePaiement;
    private Date dateCreation;
    private Date dateCompletion;

    // Informations simplifiées pour éviter les références circulaires
    private String expediteurNom;
    private String expediteurTelephone;
    private String destinataireNom;
    private String destinataireTelephone;
    private String agentNom;
    private String agentTelephone;
    private String expediteurAgentNom;
    private String expediteurAgentTelephone;
    private String destinataireAgentNom;
    private String destinataireAgentTelephone;
    private String beneficiaireNom;
}
