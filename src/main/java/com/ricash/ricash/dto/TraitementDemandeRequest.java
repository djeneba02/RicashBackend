package com.ricash.ricash.dto;

import lombok.Data;

@Data
public class TraitementDemandeRequest {
    private Long demandeId;
    private String raison; // Pour le rejet
}