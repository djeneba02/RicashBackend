package com.ricash.ricash.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class DocumentIdentiteDTO {
    private Long id;
    private String type;
    private String numero;
    private String imageRectoUrl;
    private String imageVersoUrl;
    private LocalDateTime dateValidation;
    private Date dateExpiration;
}