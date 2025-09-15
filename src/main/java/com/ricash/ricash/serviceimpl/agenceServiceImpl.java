package com.ricash.ricash.serviceimpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.dto.AgenceDTO;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Adresse;
import com.ricash.ricash.model.Agence;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agenceRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.service.agenceService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class agenceServiceImpl implements agenceService {

    private final agenceRepository agenceRepository;
    private final adminRepository adminRepository;
    private final FirebaseTokendFilter firebaseTokendFilter;
    private final agentRepository agentRepository;

    public agenceServiceImpl(agenceRepository agenceRepository, adminRepository adminRepository, FirebaseTokendFilter firebaseTokendFilter, agentRepository agentRepository) {
        this.agenceRepository = agenceRepository;
        this.adminRepository = adminRepository;
        this.firebaseTokendFilter = firebaseTokendFilter;
        this.agentRepository = agentRepository;
    }

    public AgenceDTO convertToDTO(Agence agence) {
        AgenceDTO dto = new AgenceDTO();
        dto.setId(agence.getId());
        dto.setNom(agence.getNom());
        dto.setTelephone(agence.getTelephone());
        dto.setEstActive(agence.isEstActive());

        if (agence.getAgent() != null) {
            dto.setAgentNom(agence.getAgent().getNom());
        }

        if (agence.getAdresse() != null) {
            // Créer une copie des données sans référence circulaire
            Adresse adresse = new Adresse();
            adresse.setId(agence.getAdresse().getId());
            adresse.setLigne1(agence.getAdresse().getLigne1());
            adresse.setLigne2(agence.getAdresse().getLigne2());
            adresse.setPays(agence.getAdresse().getPays());
            adresse.setVille(agence.getAdresse().getVille());
            adresse.setCodePostal(agence.getAdresse().getCodePostal());
            dto.setAdresse(adresse);
        }

        return dto;
    }


    @Override
    public AgenceDTO createAgenceByAgent(AgenceDTO request, String token) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokendFilter.findUserInAllTables(uid);
            if (userDetails == null || (!"AGENT".equalsIgnoreCase(userDetails.getRole()) && !"ROLE_AGENT".equalsIgnoreCase(userDetails.getRole()))) {
                throw new IllegalArgumentException("Seuls les agents activés peuvent créer une agence");
            }

            Agent agent = agentRepository.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            System.out.println("Agent actif: " + agent.isEstActif() + ", validé: " + agent.isEstValide());

            if (!agent.isEstActif() || !agent.isEstValide()) {
                throw new IllegalArgumentException("L'agent doit être validé et actif pour créer une agence");
            }

            if (agenceRepository.findByAgent_Id(agent.getId()).isPresent()) {
                throw new IllegalArgumentException("Cet agent a déjà une agence");
            }

            if (agenceRepository.findByNom(request.getNom()).isPresent()) {
                throw new RuntimeException("Nom d'agence déjà utilisé");
            }
            if (agenceRepository.findByTelephone(request.getTelephone()).isPresent()) {
                throw new RuntimeException("Téléphone déjà utilisé");
            }

            // Créer une nouvelle adresse pour éviter les références circulaires
            Adresse adresse = new Adresse();
            adresse.setLigne1(request.getAdresse().getLigne1());
            adresse.setLigne2(request.getAdresse().getLigne2());
            adresse.setVille(request.getAdresse().getVille());
            adresse.setCodePostal(request.getAdresse().getCodePostal());
            adresse.setPays(request.getAdresse().getPays());

            Agence agence = new Agence();
            agence.setNom(request.getNom());
            agence.setTelephone(request.getTelephone());
            agence.setAdresse(adresse);
            agence.setAgent(agent);
            agence.setEstActive(false);

            Agence savedAgence = agenceRepository.save(agence);
            return convertToDTO(savedAgence);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }


    @Override
    public List<AgenceDTO> getAllAgences() {
        return agenceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AgenceDTO toggleAgenceStatus(Long agenceId, boolean isActive, String token) {
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokendFilter.findUserInAllTables(uid);
            if (userDetails == null || !"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent modifier le statut d'une agence");
            }

            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            agence.setEstActive(isActive);
            agence.setAdmin(admin);

            Agence updatedAgence = agenceRepository.save(agence);
            return convertToDTO(updatedAgence);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }
}
