package com.ricash.ricash.serviceimpl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.config.FirebaseAuthService;
import com.ricash.ricash.config.FirebaseTokendFilter;
import com.ricash.ricash.dto.UserRegistrationRequest;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.DocumentIdentite;
import com.ricash.ricash.model.Enum.statutKYC;
import com.ricash.ricash.model.Enum.typeDocument;
import com.ricash.ricash.model.User;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.documentIdentiteRepository;
import com.ricash.ricash.repository.userRepository;
import com.ricash.ricash.service.FirebaseStorageService;
import com.ricash.ricash.service.userService;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Transactional
@Service
public class userServiceImpl implements userService {

    private final documentIdentiteRepository documentIdentiteRepository;
    private final adminRepository adminRepository;
    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseAuthService firebaseAuthService;
    private final FirebaseStorageService firebaseStorageService;
    private final FirebaseTokendFilter firebaseTokendFilter;

    public userServiceImpl(documentIdentiteRepository documentIdentiteRepository, adminRepository adminRepository, userRepository userRepository, PasswordEncoder passwordEncoder, FirebaseAuthService firebaseAuthService, FirebaseStorageService firebaseStorageService, FirebaseTokendFilter firebaseTokendFilter) {
        this.documentIdentiteRepository = documentIdentiteRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.firebaseAuthService = firebaseAuthService;
        this.firebaseStorageService = firebaseStorageService;
        this.firebaseTokendFilter = firebaseTokendFilter;
    }
    @Override
    public User registerUser(UserRegistrationRequest userRequest, MultipartFile rectoFile, MultipartFile versoFile) throws Exception {

        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Créer l'utilisateur Firebase
        UserRecord userRecord = firebaseAuthService.createFirebaseUser(
                userRequest.getEmail(),
                userRequest.getPassword(), // Note: utiliser getPassword() au lieu de getMotDePasse()
                userRequest.getNom(),
                userRequest.getPrenom(),
                userRequest.getTelephone()
        );

        // Construire l'utilisateur
        User user = new User();
        user.setUid(userRecord.getUid());
        user.setEmail(userRequest.getEmail());
        user.setMotDePasse(passwordEncoder.encode(userRequest.getPassword()));
        user.setNom(userRequest.getNom());
        user.setPrenom(userRequest.getPrenom());
        user.setTelephone(userRequest.getTelephone());
        if (userRequest.getDateNaissance() != null && !userRequest.getDateNaissance().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date dateNaissance = sdf.parse(userRequest.getDateNaissance());
                user.setDateNaissance(dateNaissance);
            } catch (ParseException e) {
                throw new RuntimeException("Format de date de naissance invalide. Utilisez yyyy-MM-dd");
            }
        }
        user.setDateCreation(LocalDateTime.now());
        user.setActif(false); // Désactivé jusqu'à validation
        user.setRole("USER");
        user.setKycStatut(statutKYC.EN_COURS);

        // Vérifier que le typeDocument est fourni
        if (userRequest.getTypeDocument() == null || userRequest.getTypeDocument().isEmpty()) {
            throw new RuntimeException("Le type de document est requis");
        }

        // Convertir le type de document string en enum
        typeDocument documentType;
        try {
            documentType = typeDocument.valueOf(userRequest.getTypeDocument().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type de document invalide: " + userRequest.getTypeDocument() +
                    ". Types valides: PASSEPORT, CARTE_IDENTITE, PERMIS_CONDUITE");
        }

        // Créer le document d'identité
        DocumentIdentite document = new DocumentIdentite();
        document.setType(documentType); // Utiliser le type du DTO
        document.setNumero(userRequest.getNumeroDocument() != null ?
                userRequest.getNumeroDocument() : generateDocumentNumber());

        document.setUtilisateur(user);

        if (rectoFile != null && !rectoFile.isEmpty()) {
            try {
                String uploadDir = "uploads/Clients/";
                String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(rectoFile.getOriginalFilename());

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(filename);
                Files.copy(rectoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/Clients/")
                        .path(filename)
                        .toUriString();

                document.setImageRectoUrl(fileUri);
            } catch (IOException e) {
                throw new RuntimeException("Échec de l'upload de l'image", e);
            }
        }
        // Gestion de l'image (upload)
        if (versoFile != null && !versoFile.isEmpty()) {
            try {
                String uploadDir = "uploads/Clients/";
                String filname = UUID.randomUUID() + "_" + StringUtils.cleanPath(versoFile.getOriginalFilename());

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filPath = uploadPath.resolve(filname);
                Files.copy(versoFile.getInputStream(), filPath, StandardCopyOption.REPLACE_EXISTING);

                String filUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/Clients/")
                        .path(filname)
                        .toUriString();

                document.setImageVersoUrl (filUri);
            } catch (IOException e) {
                throw new RuntimeException("Échec de l'upload de l'image", e);
            }
        }
        user.setDocumentsIdentite(java.util.List.of(document));

        return userRepository.save(user);
    }

    private String generateDocumentNumber() {
        String identifiant;
        do {
            long randomNumber = (long) (Math.random() * 900000L) + 100000L;
            identifiant = "DOC" + randomNumber;
        } while (documentIdentiteRepository.existsByNumero(identifiant));

        return identifiant;
    }

    @Override
    public User validateUserDocuments(UserRegistrationRequest request, boolean isValid, String token) {
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
                throw new IllegalArgumentException("Seuls les admins peuvent valider des documents");
            }

            // 4. Rechercher l'admin dans la base de données
            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            // 5. Récupérer l'utilisateur à valider
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (user.getDocumentsIdentite() != null && !user.getDocumentsIdentite().isEmpty()) {
                DocumentIdentite document = user.getDocumentsIdentite().get(0);

                if (isValid) {
                    // Validation réussie
                    user.setKycStatut(statutKYC.VERIFIE);
                    user.setActif(true);

                    // Définir la date d'expiration fournie par l'admin
                    if (request.getDateExpiration() != null) {
                        // Convertir LocalDateTime en Date si nécessaire
                        document.setDateExpiration(java.sql.Timestamp.valueOf(request.getDateExpiration()));
                    }

                    // Associer l'admin qui a fait la validation
                    user.setAdmin(admin);
                    document.setDateValidation(LocalDateTime.now());

                } else {
                    // Rejet
                    user.setKycStatut(statutKYC.REJETE);
                    user.setActif(false);
                    user.setRaisonRejet(request.getRaison());
                }
            }

            return userRepository.save(user);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }


    @Override
    public User toggleUserStatus(Long userId, boolean isActive, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

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
                throw new IllegalArgumentException("Seuls les admins peuvent modifier le statut des utilisateurs");
            }

            // 4. Rechercher l'admin dans la base de données
            Admin admin = adminRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

            // 5. Modifier le statut de l'utilisateur
            user.setActif(isActive);
            user.setAdmin(admin); // Enregistrer quel admin a fait la modification

            return userRepository.save(user);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide: " + e.getMessage());
        }
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getActiveUsers() {
        return userRepository.findByActifTrue();
    }

    @Override
    public List<User> getInactiveUsers() {
        return userRepository.findByActifFalse();
    }
}