package com.ricash.ricash.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserRegistrationRequest {
    private Long userId;
    private String email;
    private String password;
    private String nom;
    private String prenom;
    private String telephone;
    private String typeDocument;
    private String numeroDocument;
    private LocalDateTime dateExpiration;
    private String raison;
    private String dateNaissance;
    private Enum statatKYC;
}