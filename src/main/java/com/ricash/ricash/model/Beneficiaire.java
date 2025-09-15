package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.methodePaiement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Beneficiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String prenom;

    private String telephone;

    private String pays;

    @Enumerated(EnumType.STRING)
    private methodePaiement methodePaiement;

    private boolean estVerifie;

    // Relations
    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

    @OneToMany(mappedBy = "beneficiaire")
    private List<Transaction> transactions;
}
