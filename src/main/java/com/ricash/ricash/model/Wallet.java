package com.ricash.ricash.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
@Entity @Getter
@Setter @AllArgsConstructor @NoArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal solde;
    private String devise;
    private Date dateDerniereMAJ;

    // Relation avec User
    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;
}
