package com.ricash.ricash.service;

import com.ricash.ricash.model.ParametreSysteme;
import com.ricash.ricash.repository.parametreSysRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class paramettreSystService {

    private final parametreSysRepository parametreSystemeRepository;

    @PostConstruct
    public void initParametresSysteme() {
        // Vérifier si des paramètres existent déjà
        if (parametreSystemeRepository.count() == 0) {
            ParametreSysteme parametres = ParametreSysteme.builder()
                    .fraisTransfert(new BigDecimal("2.5")) // 2.5% de frais
                    .tauxChange(new BigDecimal("655.0")) // Taux XOF/USD par défaut
                    .limiteTransfert(new BigDecimal("1000000.00"))
                    .seuilVerification(new BigDecimal("500000.00"))
                    .build();

            parametreSystemeRepository.save(parametres);
            System.out.println("Paramètres système initialisés avec succès");
        }
    }
}