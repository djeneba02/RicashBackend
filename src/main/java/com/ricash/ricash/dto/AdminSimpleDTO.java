package com.ricash.ricash.dto;

import lombok.Data;

@Data
public class AdminSimpleDTO {
    private Long id;
    private String uid;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role;
    private boolean estActif;
    private String avatarUrl;
}
