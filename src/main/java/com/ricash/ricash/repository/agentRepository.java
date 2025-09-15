package com.ricash.ricash.repository;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.Enum.statutKYC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface agentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByUid(String uid);
    Optional<Agent> findById(Long Id);
    Optional <Agent> findByEmail(String email);
    boolean existsByUid(String uid);
    Optional<Agent> findByIdentifiant(String identifiant);
    boolean existsByEmail(String email);
    boolean existsByIdentifiant(String identifiant);
    List<Agent> findByEstValideTrue();
    List<Agent> findByEstValideFalseAndKycStatut(statutKYC statutKYC);
    List<Agent> findByEstActifTrue();
    List<Agent> findByEstActifFalse();
}