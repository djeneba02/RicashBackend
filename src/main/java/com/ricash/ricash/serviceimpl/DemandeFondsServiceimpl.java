package com.ricash.ricash.serviceimpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.DemandeFonds;
import com.ricash.ricash.model.Enum.StatutDemandeFonds;
import com.ricash.ricash.repository.DemandeFondsRepository;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.service.AgentFundService;
import com.ricash.ricash.service.DemandeFondsService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DemandeFondsServiceimpl implements DemandeFondsService {

    private final DemandeFondsRepository demandeFondsRepository;
    private final agentRepository agentRepository;
    private final adminRepository adminRepository;
    private final AgentFundService agentFundService;
    private final FirebaseTokendFilter firebaseTokenFilter;

    public DemandeFondsServiceimpl(DemandeFondsRepository demandeFondsRepository, agentRepository agentRepository, adminRepository adminRepository, AgentFundService agentFundService, FirebaseTokendFilter firebaseTokenFilter) {
        this.demandeFondsRepository = demandeFondsRepository;
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
        this.agentFundService = agentFundService;
        this.firebaseTokenFilter = firebaseTokenFilter;
    }

    // Agent crée une demande de fonds
    @Transactional
    public DemandeFonds creerDemandeFonds(Double montant, String motif, String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_AGENT".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les agents peuvent créer des demandes de fonds");
            }

            Agent agent = agentRepository.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            // Vérifier que l'agent est actif et validé
            if (!agent.isEstActif() || !agent.isEstValide()) {
                throw new IllegalArgumentException("L'agent doit être actif et validé pour faire une demande");
            }

            // Validation du montant
            if (montant <= 0) {
                throw new IllegalArgumentException("Le montant doit être positif");
            }

            if (montant > 10000000) { // Limite de 10 millions par exemple
                throw new IllegalArgumentException("Le montant demandé dépasse la limite autorisée");
            }

            DemandeFonds demande = DemandeFonds.builder()
                    .reference(genererReferenceDemande())
                    .montantDemande(montant)
                    .motif(motif)
                    .statut(StatutDemandeFonds.EN_ATTENTE)
                    .agent(agent)
                    .dateCreation(LocalDateTime.now())
                    .build();

            return demandeFondsRepository.save(demande);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    // Admin traite une demande (approbation)
    @Transactional
    public DemandeFonds approuverDemande(Long demandeId, String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent approuver les demandes");
            }

            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            DemandeFonds demande = demandeFondsRepository.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            if (demande.getStatut() != StatutDemandeFonds.EN_ATTENTE) {
                throw new IllegalArgumentException("La demande a déjà été traitée");
            }

            // Approvisionner la caisse de l'agent
            agentFundService.approvisionnerCaisseAgent(
                    demande.getAgent().getId(),
                    demande.getMontantDemande(),
                    token
            );

            // Mettre à jour la demande
            demande.setStatut(StatutDemandeFonds.APPROUVEE);
            demande.setAdminTraiteur(admin);
            demande.setDateTraitement(LocalDateTime.now());

            return demandeFondsRepository.save(demande);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    // Admin rejette une demande
    @Transactional
    public DemandeFonds rejeterDemande(Long demandeId, String raison, String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent rejeter les demandes");
            }

            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            DemandeFonds demande = demandeFondsRepository.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            if (demande.getStatut() != StatutDemandeFonds.EN_ATTENTE) {
                throw new IllegalArgumentException("La demande a déjà été traitée");
            }

            demande.setStatut(StatutDemandeFonds.REJETEE);
            demande.setRaisonRejet(raison);
            demande.setAdminTraiteur(admin);
            demande.setDateTraitement(LocalDateTime.now());

            return demandeFondsRepository.save(demande);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Override
    // Récupérer les demandes d'un agent
    public List<DemandeFonds> getMesDemandes(String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_AGENT".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Accès non autorisé");
            }

            Agent agent = agentRepository.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            return demandeFondsRepository.findByAgentId(agent.getId());

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Override
    // Récupérer toutes les demandes en attente (pour admin)
    public List<DemandeFonds> getDemandesEnAttente(String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent voir toutes les demandes");
            }

            return demandeFondsRepository.findByStatut(StatutDemandeFonds.EN_ATTENTE);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }
@Override
public String genererReferenceDemande() {
        return "DF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}


