package com.ricash.ricash.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Value;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.dto.LoginRequest;
import com.ricash.ricash.dto.UserRegistrationRequest;
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
    private String ApiKey;

    private final agentService agentService;
    private final userService userService;
    private final FirebaseAuthService firebaseAuthService;
    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final adminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public AuthController(agentService agentService, userService userService, FirebaseAuthService firebaseAuthService, userRepository userRepository, agentRepository agentRepository, adminRepository adminRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.userService = userService;
        this.firebaseAuthService = firebaseAuthService;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
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
    @GetMapping("/profile/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) {
        Optional<User> user = userRepository.findByUid(uid);
        Optional<Agent> agent = agentRepository.findByUid(uid);
        Optional<Admin> admin = adminRepository.findByUid(uid);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else if (agent.isPresent()) {
            return ResponseEntity.ok(agent.get());
        } else if (admin.isPresent()) {
            return ResponseEntity.ok(admin.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

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
    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestParam String idToken) {
        try {
            // Implémentez la vérification du token si nécessaire
            return ResponseEntity.ok(Map.of("valid", true, "message", "Token valide"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("valid", false, "error", "Token invalide")
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
}