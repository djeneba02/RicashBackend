package com.ricash.ricash.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "parametres_transfert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreSysteme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fraisTransfert;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal tauxChange;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limiteTransfert;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal seuilVerification;

    // Relations
    @OneToMany(mappedBy = "parametreSysteme")
    private List<Transaction> transactions;
}
