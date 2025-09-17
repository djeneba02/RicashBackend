package com.ricash.ricash.dto;

import lombok.Data;

@Data
public class DemandeFondsRequest {
    private Double montant;
    private String motif;
}