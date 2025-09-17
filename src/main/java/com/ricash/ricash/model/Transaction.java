package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.methodePaiement;
import com.ricash.ricash.model.Enum.statutTransaction;
import com.ricash.ricash.model.Enum.typeTransaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private double montant;

    private String devise;

    private double frais;

    private double montantTotal;

    private double tauxChange;

    private double codeTransaction;

    @Enumerated(EnumType.STRING)
    private statutTransaction statut;     

    @Enumerated(EnumType.STRING)
    private typeTransaction type;

    @Enumerated(EnumType.STRING)
    private methodePaiement methodePaiement;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCompletion;

    // Relations
    @ManyToOne
    @JoinColumn(name = "expediteur_id")
    private User expediteur;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private User destinataire;

    @ManyToOne @JoinColumn(name = "expediteur_agent_id")
    private Agent expediteurAgent;

    @ManyToOne @JoinColumn(name = "destinataire_agent_id")
    private Agent destinataireAgent;

    @ManyToOne
    @JoinColumn(name = "beneficiaire_id")
    private Beneficiaire beneficiaire;

    @ManyToOne @JoinColumn(name = "agent_operateur_id")
    private Agent agent;

    @ManyToOne
    @JoinColumn(name = "parametre_systeme_id")
    private ParametreSysteme parametreSysteme;
}
