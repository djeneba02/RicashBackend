package com.ricash.ricash.dto;

import lombok.Data;

@Data
public class AdresseDTO {
    private Long id;
    private String ligne1;
    private String ligneé;
    private String ville;
    private String codePostal;
    private String pays;
}