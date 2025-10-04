package com.ricash.ricash.repository;

import com.ricash.ricash.model.Agence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface agenceRepository extends JpaRepository<Agence, Long> {
    Optional<Agence> findByNom(String nom);
    Optional<Agence> findByTelephone(String telephone);
    Optional<Agence> findByAgent_Id(Long agentId);

    List<Agence> findByEstActive(boolean estActive);
}
