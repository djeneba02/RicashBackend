package com.ricash.ricash.repository;

import com.ricash.ricash.model.DemandeFonds;
import com.ricash.ricash.model.Enum.StatutDemandeFonds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeFondsRepository extends JpaRepository<DemandeFonds, Long> {
    List<DemandeFonds> findByAgentId(Long agentId);
    List<DemandeFonds> findByStatut(StatutDemandeFonds statut);
    List<DemandeFonds> findByAgentIdAndStatut(Long agentId, StatutDemandeFonds statut);
    Optional<DemandeFonds> findByReference(String reference);
}
