package com.ricash.ricash.serviceimpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.config.FirebaseAuthService;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.dto.AgentValidationRequest;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.Enum.statutKYC;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.service.FirebaseStorageService;
import com.ricash.ricash.service.agentService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Transactional
@Service
public class agentServiceImpl implements agentService {

    private final agentRepository agentRepository;
    private final adminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseAuthService firebaseAuthService;
    private final FirebaseStorageService firebaseStorageService;
    private final FirebaseTokendFilter firebaseTokendFilter;

    public agentServiceImpl(agentRepository agentRepository, adminRepository adminRepository, PasswordEncoder passwordEncoder, FirebaseAuthService firebaseAuthService, FirebaseStorageService firebaseStorageService, FirebaseTokendFilter firebaseTokendFilter) {
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.firebaseAuthService = firebaseAuthService;
        this.firebaseStorageService = firebaseStorageService;
        this.firebaseTokendFilter = firebaseTokendFilter;
    }


    public Agent registerAgent(Agent request, MultipartFile file,  MultipartFile fil)
            throws FirebaseAuthException, IOException {

        // Vérifier si l'email ou l'identifiant existe déjà
        if (agentRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        if (agentRepository.findByIdentifiant(request.getIdentifiant()).isPresent()) {
            throw new RuntimeException("Identifiant déjà utilisé");
        }

        // Créer l'utilisateur Firebase
        UserRecord userRecord = firebaseAuthService.createFirebaseUser(
                request.getEmail(),
                request.getMotDePasse(), // se
                request.getNom(),
                request.getPrenom(),
                request.getTelephone()
        );

        String identifiantAuto = generateUniqueIdentifiant();
        // Construire l'agent
        Agent agent = new Agent();
        agent.setUid(userRecord.getUid());
        agent.setEmail(request.getEmail());
        agent.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        agent.setIdentifiant(identifiantAuto);
        agent.setNom(request.getNom());
        agent.setPrenom(request.getPrenom());
        agent.setTelephone(request.getTelephone());
        agent.setEstActif(false);
        agent.setEstValide(false);
        agent.setSoldeCaisse(0.0);
        agent.setKycStatut(statutKYC.EN_COURS);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setRole("AGENT");

        // Gestion de l'image (upload)
        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "uploads/Agents/";
                String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/Agents/")
                        .path(filename)
                        .toUriString();

                agent.setImageRectoUrl(fileUri);
            } catch (IOException e) {
                throw new RuntimeException("Échec de l'upload de l'image", e);
            }
        }
        // Gestion de l'image (upload)
        if (fil != null && !fil.isEmpty()) {
            try {
                String uploadDir = "uploads/Agents/";
                String filname = UUID.randomUUID() + "_" + StringUtils.cleanPath(fil.getOriginalFilename());

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filPath = uploadPath.resolve(filname);
                Files.copy(file.getInputStream(), filPath, StandardCopyOption.REPLACE_EXISTING);

                String filUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/Agents/")
                        .path(filname)
                        .toUriString();

                agent.setImageVersoUrl(filUri);
            } catch (IOException e) {
                throw new RuntimeException("Échec de l'upload de l'image", e);
            }
        }

        return agentRepository.save(agent);
    }

    private String generateUniqueIdentifiant() {
        String identifiant;
        do {
            // Générer un identifiant de format "AGT" + 6 chiffres
            long randomNumber = (long) (Math.random() * 900000L) + 100000L;
            identifiant = "AGT" + randomNumber;
        } while (agentRepository.findByIdentifiant(identifiant).isPresent());

        return identifiant;
    }


    public Agent validateAgent(AgentValidationRequest request, String token) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        try {
            // 1. Décoder le token Firebase pour obtenir l'UID
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            // 2. Récupérer les informations de l'utilisateur connecté via l'UID
            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokendFilter.findUserInAllTables(uid);

            if (userDetails == null) {
                throw new IllegalArgumentException("Utilisateur non trouvé");
            }

            // 3. Vérifier si l'utilisateur est un ADMIN
            if (!"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent valider des agents");
            }

            // 4. Rechercher l'admin dans la base de données
            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            if (request.isValidation()) {
                agent.setEstValide(true);
                agent.setEstActif(true);
                agent.setAdmin(admin);
                agent.setKycStatut(statutKYC.VERIFIE);
                agent.setRaisonRejet(null);
            } else {
                agent.setEstValide(false);
                agent.setEstActif(false);
                agent.setKycStatut(statutKYC.REJETE);
                agent.setRaisonRejet(request.getRaison());
            }

            agent.setUpdatedAt(LocalDateTime.now());
            return agentRepository.save(agent);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }


    public List<Agent> getAgentsEnAttente() {
        return agentRepository.findByEstValideFalseAndKycStatut(statutKYC.EN_COURS);
    }

    public List<Agent> getAgentsValides() {
        return agentRepository.findByEstValideTrue();
    }

    @Override
    public Agent toggleAgentStatus(Long agentId, boolean isActive, String token) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        try {
            // 1. Décoder le token Firebase pour obtenir l'UID
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            // 2. Récupérer les informations de l'utilisateur connecté via l'UID
            FirebaseTokendFilter.UserDetailsWrapper userDetails = firebaseTokendFilter.findUserInAllTables(uid);

            if (userDetails == null) {
                throw new IllegalArgumentException("Utilisateur non trouvé");
            }

            // 3. Vérifier si l'utilisateur est un ADMIN
            if (!"ROLE_ADMIN".equals(userDetails.getRole())) {
                throw new IllegalArgumentException("Seuls les admins peuvent modifier le statut des agents");
            }

            // 4. Rechercher l'admin dans la base de données
            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            // 5. Modifier le statut de l'agent
            agent.setEstActif(isActive);
            agent.setAdmin(admin); // Enregistrer quel admin a fait la modification
            agent.setUpdatedAt(LocalDateTime.now());

            return agentRepository.save(agent);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Override
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
}
