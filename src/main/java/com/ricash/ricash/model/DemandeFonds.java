package com.ricash.ricash.model;


import com.ricash.ricash.model.Enum.StatutDemandeFonds;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_fonds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeFonds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private Double montantDemande;

    private String motif;

    @Enumerated(EnumType.STRING)
    private StatutDemandeFonds statut;

    private String raisonRejet;

    private LocalDateTime dateCreation;

    private LocalDateTime dateTraitement;

    // Relations
    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin adminTraiteur;
}