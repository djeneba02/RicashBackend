package com.ricash.ricash.dto;

import com.ricash.ricash.model.Enum.statutKYC;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class UserResponseDTO {
    private Long id;
    private String uid;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDateTime dateCreation;
    private Date dateNaissance;
    private statutKYC kycStatut;
    private boolean actif;
    private String role;
    private String raisonRejet;

    // DTOs imbriqués pour éviter les références circulaires
    private AdresseDTO adresse;
    private WalletDTO portefeuille;
    private List<DocumentIdentiteDTO> documentsIdentite;
    private AdminSimpleDTO admin; // Seulement les infos basiques de l'admin
}
