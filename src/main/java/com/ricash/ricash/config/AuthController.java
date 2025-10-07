package com.ricash.ricash.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Value;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.Mappe.AdminMapper;
import com.ricash.ricash.dto.*;
import com.ricash.ricash.Mappe.UserMapper;
import com.ricash.ricash.Mappe.AgentMapper;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.User;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.repository.userRepository;
import com.ricash.ricash.service.agentService;
import com.ricash.ricash.service.userService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${firebase.api.key}")
    private final String ApiKey = "AIzaSyDYKYGtV5spNjorZP7sQB9JAfM1wnepqek";

    private final FirebaseAuth firebaseAuth;
    private final agentService agentService;
    private final userService userService;
    private final FirebaseAuthService firebaseAuthService;
    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final adminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final AdminMapper adminMapper;

    public AuthController(FirebaseAuth firebaseAuth, agentService agentService, userService userService, FirebaseAuthService firebaseAuthService, userRepository userRepository, agentRepository agentRepository, adminRepository adminRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper, UserMapper userMapper, AgentMapper agentMapper, AdminMapper adminMapper) {
        this.firebaseAuth = firebaseAuth;
        this.agentService = agentService;
        this.userService = userService;
        this.firebaseAuthService = firebaseAuthService;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.adminMapper = adminMapper;
    }

    @PostMapping(value = "/register/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerUser(
            @RequestPart("user") String userJson,
            @RequestPart(value = "rectoFile", required = false) MultipartFile rectoFile,
            @RequestPart(value = "versoFile", required = false) MultipartFile versoFile) {

        try {
            // Convertir le JSON en DTO UserRegistrationRequest
            UserRegistrationRequest userRequest = objectMapper.readValue(userJson, UserRegistrationRequest.class);

            // Vérifier si l'email existe déjà
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Un utilisateur avec cet email existe déjà");
            }

            User createdUser = userService.registerUser(userRequest, rectoFile, versoFile);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully. Waiting for document validation.");
            response.put("uid", createdUser.getUid());
            response.put("role", "USER");
            response.put("status", "PENDING_VALIDATION");

            if (createdUser.getAdresse() != null) {
                response.put("adresseCree", true);
            }

            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Erreur de format JSON");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }

    // Register Admin
    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String telephone) {

        try {
            UserRecord userRecord = firebaseAuthService.createFirebaseUser(email, password, nom, prenom, telephone);

            // Sauvegarder dans MySQL
            Admin newAdmin = new Admin();
            newAdmin.setUid(userRecord.getUid());
            newAdmin.setEmail(email);
            newAdmin.setNom(nom);
            newAdmin.setPrenom(prenom);
            newAdmin.setTelephone(telephone);
            newAdmin.setCreatedAt(LocalDateTime.now());
            newAdmin.setRole("ADMIN");
            newAdmin.setMotDePasse(passwordEncoder.encode(password));

            adminRepository.save(newAdmin);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin registered successfully");
            response.put("uid", userRecord.getUid());
            response.put("role", "ADMIN");

            return ResponseEntity.ok(response);

        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }


    @PostMapping(value = "/register/agent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerAgent(
            @RequestPart("agent") String agentJson,
            @RequestPart(value = "file", required = false) MultipartFile file, MultipartFile fil) {

        try {
            // Convertir le JSON en objet Agent
            Agent agent = objectMapper.readValue(agentJson, Agent.class);

            // Vérifier si l'email existe déjà
            if (agentRepository.existsByEmail(agent.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Un agent avec cet email existe déjà");
            }

            Agent createdAgent = agentService.registerAgent(agent, file, fil);
            return ResponseEntity.ok(createdAgent);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Erreur de format JSON");
        } catch (FirebaseAuthException e) {
            return ResponseEntity.internalServerError().body("Erreur Firebase : " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur d'upload fichier : " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }




// Endpoint pour vérifier le type d'utilisateur
    @GetMapping("/user-type/{uid}")
    public ResponseEntity<?> getUserType(@PathVariable String uid) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.findByUid(uid).isPresent()) {
            response.put("role", "USER");
        } else if (agentRepository.findByUid(uid).isPresent()) {
            response.put("role", "AGENT");
        } else if (adminRepository.findByUid(uid).isPresent()) {
            response.put("role", "ADMIN");
        } else {
            return ResponseEntity.notFound().build();
        }

        response.put("uid", uid);
        return ResponseEntity.ok(response);
    }

    // Get user profile
//    @GetMapping("/profile/{uid}")
//    public ResponseEntity<?> getProfile(@PathVariable String uid) {
//        Optional<User> user = userRepository.findByUid(uid);
//        Optional<Agent> agent = agentRepository.findByUid(uid);
//        Optional<Admin> admin = adminRepository.findByUid(uid);
//
//        if (user.isPresent()) {
//            return ResponseEntity.ok(user.get());
//        } else if (agent.isPresent()) {
//            return ResponseEntity.ok(agent.get());
//        } else if (admin.isPresent()) {
//            return ResponseEntity.ok(admin.get());
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Map<String, Object> response = firebaseAuthService.loginWithEmail(
                    loginRequest.getEmail(),
                    loginRequest.getPassword(),
                    loginRequest.getUserType()
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Type d'utilisateur invalide", "message", e.getMessage())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Échec de l'authentification", "message", e.getMessage())
            );
        }
    }



    // Endpoint pour détection automatique du type d'utilisateur
    @PostMapping("/auto-login")
    public ResponseEntity<?> autoLogin(@RequestParam String email, @RequestParam String password) {
        try {
            // Essayer les différents types d'utilisateurs
            String[] userTypes = {"USER", "AGENT", "ADMIN"};

            for (String userType : userTypes) {
                try {
                    Map<String, Object> response = firebaseAuthService.loginWithEmail(email, password, userType);
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    // Continuer avec le type suivant
                    continue;
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Aucun utilisateur trouvé avec ces identifiants")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Erreur serveur", "message", e.getMessage())
            );
        }
    }

    // Endpoint pour vérifier le token
// Dans AuthController.java - Remplacer l'endpoint verify-token
    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Token valide",
                    "uid", decodedToken.getUid()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("valid", false, "error", "Token invalide: " + e.getMessage())
            );
        }
    }

    // Endpoint pour rafraîchir le token
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            String url = "https://securetoken.googleapis.com/v1/token?key=" + ApiKey;

            Map<String, String> request = new HashMap<>();
            request.put("grant_type", "refresh_token");
            request.put("refresh_token", refreshToken);

            ResponseEntity<Map> response = new RestTemplate().postForEntity(url, request, Map.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Échec du rafraîchissement du token")
            );
        }
    }



    // Récupérer le profil de l'utilisateur connecté
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
            String uid = decodedToken.getUid();

            return getProfile(uid); // Réutilise votre méthode existante

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Token invalide", "message", e.getMessage())
            );
        }
    }

    // Mettre à jour le profil utilisateur
    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "").trim();
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
            String uid = decodedToken.getUid();

            // Trouver l'utilisateur par UID
            Optional<User> userOpt = userRepository.findByUid(uid);
            Optional<Agent> agentOpt = agentRepository.findByUid(uid);
            Optional<Admin> adminOpt = adminRepository.findByUid(uid);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                updateUserFromMap(user, updates);
                User savedUser = userRepository.save(user);
                UserResponseDTO responseDTO = userMapper.toDto(savedUser);
                return ResponseEntity.ok(responseDTO);

            } else if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                updateAgentFromMap(agent, updates);
                Agent savedAgent = agentRepository.save(agent);
                AgentDTO responseDTO = agentMapper.toDto(savedAgent);
                return ResponseEntity.ok(responseDTO);

            } else if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                updateAdminFromMap(admin, updates);
                Admin savedAdmin = adminRepository.save(admin);
                AdminSimpleDTO responseDTO = adminMapper.toDto(savedAdmin);
                return ResponseEntity.ok(responseDTO);
            }

            return ResponseEntity.notFound().build();

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token invalide", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour", "message", e.getMessage()));
        }
    }

    // Méthodes helper pour la mise à jour
    private void updateUserFromMap(User user, Map<String, Object> updates) {
        if (updates.containsKey("nom")) user.setNom((String) updates.get("nom"));
        if (updates.containsKey("prenom")) user.setPrenom((String) updates.get("prenom"));
        if (updates.containsKey("telephone")) user.setTelephone((String) updates.get("telephone"));
        if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
    }

    private void updateAgentFromMap(Agent agent, Map<String, Object> updates) {
        if (updates.containsKey("nom")) agent.setNom((String) updates.get("nom"));
        if (updates.containsKey("prenom")) agent.setPrenom((String) updates.get("prenom"));
        if (updates.containsKey("telephone")) agent.setTelephone((String) updates.get("telephone"));
        if (updates.containsKey("email")) agent.setEmail((String) updates.get("email"));
    }

    private void updateAdminFromMap(Admin admin, Map<String, Object> updates) {
        if (updates.containsKey("nom")) admin.setNom((String) updates.get("nom"));
        if (updates.containsKey("prenom")) admin.setPrenom((String) updates.get("prenom"));
        if (updates.containsKey("telephone")) admin.setTelephone((String) updates.get("telephone"));
        if (updates.containsKey("email")) admin.setEmail((String) updates.get("email"));
    }

    @GetMapping("/profile/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) {
        Optional<User> user = userRepository.findByUid(uid);
        if (user.isPresent()) {
            return ResponseEntity.ok(userMapper.toDto(user.get()));
        }

        Optional<Agent> agent = agentRepository.findByUid(uid);
        if (agent.isPresent()) {
            return ResponseEntity.ok(agentMapper.toDto(agent.get()));
        }
        Optional<Admin> admin = adminRepository.findByUid(uid);
        if (admin.isPresent()) {
            return ResponseEntity.ok(adminMapper.toDto(admin.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> passwordData,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "").trim();
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
            String uid = decodedToken.getUid();
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            // Trouver l'utilisateur
            Optional<User> userOpt = userRepository.findByUid(uid);
            Optional<Agent> agentOpt = agentRepository.findByUid(uid);
            Optional<Admin> adminOpt = adminRepository.findByUid(uid);

            Object user = null;
            if (userOpt.isPresent()) user = userOpt.get();
            else if (agentOpt.isPresent()) user = agentOpt.get();
            else if (adminOpt.isPresent()) user = adminOpt.get();
            else {
                return ResponseEntity.notFound().build();
            }

            // Vérifier le mot de passe actuel
            String storedPassword = getPasswordFromUser(user);
            if (!passwordEncoder.matches(currentPassword, storedPassword)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Mot de passe actuel incorrect")
                );
            }

            // 🔥 METTRE À JOUR FIREBASE AUSSI
            try {
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                        .setPassword(newPassword);
                firebaseAuth.updateUser(request);
            } catch (FirebaseAuthException e) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Erreur Firebase: " + e.getMessage())
                );
            }

            // Mettre à jour la base MySQL
            if (user instanceof User) {
                ((User) user).setMotDePasse(passwordEncoder.encode(newPassword));
                userRepository.save((User) user);
            } else if (user instanceof Agent) {
                ((Agent) user).setMotDePasse(passwordEncoder.encode(newPassword));
                agentRepository.save((Agent) user);
            } else if (user instanceof Admin) {
                ((Admin) user).setMotDePasse(passwordEncoder.encode(newPassword));
                adminRepository.save((Admin) user);
            }

            return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Erreur lors du changement de mot de passe", "message", e.getMessage())
            );
        }
    }
    // Méthode helper pour récupérer le mot de passe
    private String getPasswordFromUser(Object user) {
        if (user instanceof User) return ((User) user).getMotDePasse();
        if (user instanceof Agent) return ((Agent) user).getMotDePasse();
        if (user instanceof Admin) return ((Admin) user).getMotDePasse();
        return null;
    }
}