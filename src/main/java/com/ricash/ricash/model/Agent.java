package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.statutKYC;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uid;

    @Column(nullable = false, unique = true)
    private String identifiant;

    @Column(unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String telephone;

    private String imageRectoUrl;

    private String imageVersoUrl;

    @Column(nullable = false)
    private Double soldeCaisse;

    @Enumerated(EnumType.STRING)
    private statutKYC kycStatut;

    private boolean estActif;

    @Column(nullable = false)
    private boolean estValide;

    private String raisonRejet;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String role = "AGENT";

    // Relations
    @OneToOne(mappedBy = "agent")
    private Agence agence;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @OneToMany(mappedBy = "agent")
    private List<Transaction> transactionsTraitees;
}
