package com.ricash.ricash.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Adresse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La ligne 1 est obligatoire")
    private String ligne1;

    private String ligne2; // optionnelle

    @NotBlank(message = "La ville est obligatoire")
    private String ville;

    private String codePostal;

    @NotBlank(message = "Le pays est obligatoire")
    private String pays;

    // Relation avec User
    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;
}
