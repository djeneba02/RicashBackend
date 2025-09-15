package com.ricash.ricash.dto;

import com.ricash.ricash.model.Adresse;
import lombok.Data;

@Data
public class AgenceDTO {
    private Long id;
    private String nom;
    private String telephone;
    private Adresse adresse;
    private boolean estActive;
    private String agentNom; // juste le nom de l’agent
}
