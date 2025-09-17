package com.ricash.ricash.service;

import com.ricash.ricash.model.DemandeFonds;

import java.util.List;

public interface DemandeFondsService {
    DemandeFonds creerDemandeFonds(Double montant, String motif, String token);
    DemandeFonds approuverDemande(Long demandeId, String token);
    DemandeFonds rejeterDemande(Long demandeId, String raison, String token);
    List<DemandeFonds> getMesDemandes(String token);
    List<DemandeFonds> getDemandesEnAttente(String token);
    String genererReferenceDemande();
}
