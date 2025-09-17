package com.ricash.ricash.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentFundService {

    private final agentRepository agentRepository;
    private final adminRepository adminRepository;
    private final FirebaseTokendFilter firebaseTokenFilter;

    @Transactional
    public Agent approvisionnerCaisseAgent(Long agentId, Double montant, String token) {
        try {
            // Vérification de l'authentification admin
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent approvisionner les caisses");
            }

            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            // Recherche de l'agent
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            // Vérification que l'agent est validé et actif
            if (!agent.isEstValide() || !agent.isEstActif()) {
                throw new IllegalArgumentException("L'agent doit être validé et actif pour recevoir des fonds");
            }

            // Vérification du montant
            if (montant <= 0) {
                throw new IllegalArgumentException("Le montant doit être positif");
            }

            // Ajout du montant à la caisse de l'agent
            double nouveauSolde = agent.getSoldeCaisse() + montant;
            agent.setSoldeCaisse(nouveauSolde);
            agent.setUpdatedAt(LocalDateTime.now());

            // Journaliser l'opération (vous pouvez créer une table pour ça)
            System.out.println("Admin " + admin.getNom() + " a approvisionné l'agent " +
                    agent.getNom() + " de " + montant + " FCFA");

            return agentRepository.save(agent);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Transactional
    public Agent retirerCaisseAgent(Long agentId, Double montant, String token) {
        try {
            // Vérification de l'authentification admin
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent retirer des caisses");
            }

            // Recherche de l'agent
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            // Vérification du solde
            if (agent.getSoldeCaisse() < montant) {
                throw new IllegalArgumentException("Solde insuffisant pour le retrait");
            }

            if (montant <= 0) {
                throw new IllegalArgumentException("Le montant doit être positif");
            }

            // Retrait du montant
            double nouveauSolde = agent.getSoldeCaisse() - montant;
            agent.setSoldeCaisse(nouveauSolde);
            agent.setUpdatedAt(LocalDateTime.now());

            return agentRepository.save(agent);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Transactional
    public Double consulterSoldeAgent(Long agentId, String token) {
        try {
            // Vérification de l'authentification
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokenFilter.findUserInAllTables(uid);
            if (userDetails == null) {
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            // Un admin peut consulter n'importe quel agent, un agent seulement son propre solde
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            if ("ROLE_AGENT".equals(userDetails.getRole()) &&
                    !agent.getUid().equals(uid)) {
                throw new IllegalArgumentException("Un agent ne peut consulter que son propre solde");
            }

            return agent.getSoldeCaisse();

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }
}