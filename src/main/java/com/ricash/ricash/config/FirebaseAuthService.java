package com.ricash.ricash.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.Mappe.AdminMapper;
import com.ricash.ricash.Mappe.UserMapper;
import com.ricash.ricash.Mappe.AgentMapper;
import com.ricash.ricash.model.User;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.repository.userRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseAuthService {

//    @Value("${firebase.api.key}")
//    private String firebaseApiKey;

    private final String firebaseApiKey = "AIzaSyDYKYGtV5spNjorZP7sQB9JAfM1wnepqek";

    private final FirebaseAuth firebaseAuth;
    private final adminRepository adminRepository;
    private final agentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final userRepository userRepository;
    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final AdminMapper adminMapper;

    public FirebaseAuthService(FirebaseAuth firebaseAuth, adminRepository adminRepository, agentRepository agentRepository, PasswordEncoder passwordEncoder, userRepository userRepository, UserMapper userMapper, AgentMapper agentMapper, AdminMapper adminMapper) {
        this.firebaseAuth = firebaseAuth;
        this.adminRepository = adminRepository;
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.agentMapper = agentMapper;
        this.adminMapper = adminMapper;
    }



public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
    return firebaseAuth.verifyIdToken(idToken);
}

    public UserRecord createFirebaseUser(String email, String password, String nom, String prenom, String telephone)
            throws FirebaseAuthException {

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(nom + " " + prenom);

        UserRecord userRecord = firebaseAuth.createUser(request);

        return userRecord;
    }


    public Map<String, Object> loginWithEmail(String email, String password, String userType) throws Exception {
        try {
            System.out.println("=== DÉBUT LOGIN ===");
            System.out.println("Email: " + email + ", Type: " + userType);

            // 1. Vérification MySQL selon le type
            Object user = findUserByEmailAndType(email, userType);
            System.out.println("Utilisateur trouvé en DB: " + (user != null ? "OUI" : "NON"));

            if (user == null) {
                throw new Exception("Utilisateur non trouvé");
            }

            // 2. Vérification état
            if (!isUserActive(user)) {
                throw new Exception("Compte désactivé. Contactez l'administrateur.");
            }

            // 3. Vérification mot de passe
            String storedPassword = getPasswordFromUser(user);
            if (storedPassword == null || storedPassword.isEmpty()) {
                throw new Exception("Mot de passe non configuré");
            }

            if (!passwordEncoder.matches(password, storedPassword)) {
                throw new Exception("Mot de passe incorrect");
            }
            System.out.println("Mot de passe vérifié: OK");

            // 4. Génération custom token
            String uid = getUidFromUser(user);
            System.out.println("UID pour custom token: " + uid);

            // VÉRIFIER QUE L'UTILISATEUR EXISTE DANS FIREBASE
            try {
                UserRecord firebaseUser = firebaseAuth.getUser(uid);
                System.out.println("Utilisateur trouvé dans Firebase: " + firebaseUser.getEmail());
            } catch (FirebaseAuthException e) {
                System.err.println("ERREUR: Utilisateur non trouvé dans Firebase: " + e.getMessage());
                throw new Exception("Utilisateur non configuré dans Firebase");
            }

            // CORRECTION: Supprimer la déclaration dupliquée de 'claims'
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", getRoleFromUser(user));
            claims.put("userId", getIdFromUser(user));
            System.out.println("Claims: " + claims);

            String customToken;
            try {
                // SUPPRIMER cette ligne qui cause l'erreur:
                // Map<String, Object> claims = new HashMap<>(); // ← À SUPPRIMER
                customToken = firebaseAuth.createCustomToken(uid, claims);
                System.out.println("Custom token généré avec succès, longueur: " + customToken.length());
                System.out.println("Token (premiers 50 caractères): " + customToken.substring(0, Math.min(50, customToken.length())) + "...");
            } catch (FirebaseAuthException e) {
                System.err.println("Échec création custom token: " + e.getErrorCode() + " - " + e.getMessage());
                throw new Exception("Erreur interne d'authentification: " + e.getMessage(), e);
            }

            // 5. Échange contre ID token
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + firebaseApiKey;
            System.out.println("URL API Firebase: " + url);

            Map<String, String> request = new HashMap<>();
            request.put("token", customToken);
            request.put("returnSecureToken", "true");
            System.out.println("Requête envoyée à Firebase: " + request);

            try {
                ResponseEntity<Map> response = new RestTemplate().postForEntity(url, request, Map.class);
                System.out.println("Statut réponse Firebase: " + response.getStatusCode());
                System.out.println("Corps réponse Firebase: " + response.getBody());

                if (response.getStatusCode() != HttpStatus.OK || !response.getBody().containsKey("idToken")) {
                    System.err.println("Échec échange token - Body: " + response.getBody());
                    throw new Exception("Firebase a rejeté le token: " + response.getBody());
                }

                String idToken = (String) response.getBody().get("idToken");
                System.out.println("ID token reçu, longueur: " + idToken.length());
                System.out.println("=== LOGIN RÉUSSI ===");

                return buildResponse(user, idToken, customToken);

            } catch (Exception e) {
                System.err.println("Erreur lors de l'appel Firebase API: " + e.getMessage());
                throw new Exception("Erreur de communication avec Firebase: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            System.err.println("=== ERREUR LOGIN ===");
            System.err.println("Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            throw e;
        }
    }

    // Méthodes helper simplifiées
    private Object findUserByEmailAndType(String email, String userType) {
        return switch (userType.toUpperCase()) {
            case "USER" -> userRepository.findByEmail(email).orElse(null);
            case "AGENT" -> agentRepository.findByEmail(email).orElse(null);
            case "ADMIN" -> adminRepository.findByEmail(email).orElse(null);
            default -> null;
        };
    }

    private boolean isUserActive(Object user) {
        if (user instanceof User) return ((User) user).isActif();
        if (user instanceof Agent) return ((Agent) user).isEstActif();
        if (user instanceof Admin) return ((Admin) user).isEstActif();
        return false;
    }

    private String getPasswordFromUser(Object user) {
        if (user instanceof User) return ((User) user).getMotDePasse();
        if (user instanceof Agent) return ((Agent) user).getMotDePasse();
        if (user instanceof Admin) return ((Admin) user).getMotDePasse();
        return null;
    }

    private String getUidFromUser(Object user) {
        if (user instanceof User) return ((User) user).getUid();
        if (user instanceof Agent) return ((Agent) user).getUid();
        if (user instanceof Admin) return ((Admin) user).getUid();
        return "";
    }

    private String getRoleFromUser(Object user) {
        if (user instanceof User) return ((User) user).getRole();
        if (user instanceof Agent) return ((Agent) user).getRole();
        if (user instanceof Admin) return ((Admin) user).getRole();
        return "";
    }

    private Long getIdFromUser(Object user) {
        if (user instanceof User) return ((User) user).getId();
        if (user instanceof Agent) return ((Agent) user).getId();
        if (user instanceof Admin) return ((Admin) user).getId();
        return 0L;
    }

//    private Map<String, Object> buildResponse(Object user, Object idToken, String customToken) {
//        Map<String, Object> response = new HashMap<>();
//
//        response.put("userId", getIdFromUser(user));
//        response.put("customToken", customToken);
//        response.put("idToken", idToken);
//        response.put("email", getEmailFromUser(user));
//        response.put("role", getRoleFromUser(user));
//        response.put("isActif", isUserActive(user));
//
//        // Ajouter les données spécifiques
//        if (user instanceof User u) {
//            response.put("userData", Map.of(
//                    "nom", u.getNom(),
//                    "prenom", u.getPrenom(),
//                    "telephone", u.getTelephone()
//            ));
//        } else if (user instanceof Agent a) {
//            response.put("userData", Map.of(
//                    "nom", a.getNom(),
//                    "prenom", a.getPrenom(),
//                    "telephone", a.getTelephone(),
//                    "identifiant", a.getIdentifiant()
//            ));
//        } else if (user instanceof Admin admin) {
//            response.put("userData", Map.of(
//                    "nom", admin.getNom(),
//                    "prenom", admin.getPrenom(),
//                    "telephone", admin.getTelephone()
//            ));
//        }
//
//        return response;
//    }

    private Map<String, Object> buildResponse(Object user, Object idToken, String customToken) {
        if (user instanceof User) {
            return userMapper.toLoginResponse((User) user, (String) idToken, customToken);
        } else if (user instanceof Agent) {
            return agentMapper.toLoginResponse((Agent) user, (String) idToken, customToken);
        } else if (user instanceof Admin) {
            return adminMapper.toLoginResponse((Admin) user, (String) idToken, customToken);
        }
        throw new IllegalArgumentException("Type d'utilisateur non supporté");
    }


    private String getEmailFromUser(Object user) {
        if (user instanceof User) return ((User) user).getEmail();
        if (user instanceof Agent) return ((Agent) user).getEmail();
        if (user instanceof Admin) return ((Admin) user).getEmail();
        return "";
    }
}

