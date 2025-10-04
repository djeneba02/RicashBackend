package com.ricash.ricash.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class WalletDTO {
    private Long id;
    private BigDecimal solde;
    private String devise;
    private Date dateDerniereMAJ;
}
