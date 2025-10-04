package com.ricash.ricash.dto;

import com.ricash.ricash.model.Adresse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor @NoArgsConstructor
public class AgenceDTO {
    private Long id;
    private String nom;
    private String telephone;
    private AdresseDTO adresse; // Utiliser AdresseDTO au lieu de Adresse
    private boolean estActive;
    private String agentNom;
    private Double solde; // Ajout du solde


}