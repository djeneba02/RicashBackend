package com.ricash.ricash.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "agences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "adresse_id", referencedColumnName = "id")
    private Adresse adresse;

    @Column(nullable = false, unique = true)
    private String telephone;

    private boolean estActive;

    // Relations
    @OneToOne
    @JoinColumn(name = "agent_id", referencedColumnName = "id")
    private Agent agent;
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;
}
