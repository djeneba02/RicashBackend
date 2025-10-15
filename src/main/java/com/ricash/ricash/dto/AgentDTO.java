package com.ricash.ricash.dto;

import com.ricash.ricash.model.Enum.statutKYC;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor @NoArgsConstructor
public class AgentDTO {
    private Long id;
    private String uid;
    private String identifiant;
    private String email;
    private String nom;
    private String prenom;
    private String telephone;
    private String imageRectoUrl;
    private String imageVersoUrl;
    private Double soldeCaisse;
    private statutKYC kycStatut;
    private boolean estActif;
    private boolean estValide;
    private String raisonRejet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String role;
    private String avatarUrl;

    // Informations minimales de l'admin pour éviter la récursion
    private AdminSimpleDTO admin;
}

