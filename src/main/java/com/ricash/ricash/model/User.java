package com.ricash.ricash.model;

import com.ricash.ricash.model.Enum.statutKYC;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "utilisateur")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(unique = true, length = 150)
    private String email;

    private String telephone;

    private String motDePasse;

    private LocalDateTime dateCreation;

    private Date dateNaissance;

    @Enumerated(EnumType.STRING)
    private statutKYC kycStatut;

//
//    private String raison;

    @Column(name = "is_actif")
    private boolean actif;

    @Column(nullable = false)
    private String role = "USER";

    // Relations
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    private String raisonRejet;

    // Relations
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "utilisateur")
    private Adresse adresse;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "utilisateur")
    private Wallet portefeuille;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private List<DocumentIdentite> documentsIdentite;

    @OneToMany(mappedBy = "expediteur")
    private List<Transaction> transactionsEnvoyees;

    @OneToMany(mappedBy = "destinataire")
    private List<Transaction> transactionsRecues;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private List<Beneficiaire> beneficiaires;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private List<Notification> notifications;

}
