package com.ricash.ricash.repository;

import com.ricash.ricash.model.DocumentIdentite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface documentIdentiteRepository extends JpaRepository<DocumentIdentite, Long> {
    boolean existsByNumero(String identifiant);
}
