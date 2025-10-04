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
        // 1. Vérification MySQL selon le type
        Object user = findUserByEmailAndType(email, userType);

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

        // 4. Génération custom token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", getRoleFromUser(user));
        claims.put("userId", getIdFromUser(user));

        String uid = getUidFromUser(user);
        String customToken = firebaseAuth.createCustomToken(uid, claims);

        // 5. Échange contre ID token
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + firebaseApiKey;

        Map<String, String> request = new HashMap<>();
        request.put("token", customToken);
        request.put("returnSecureToken", "true");

        ResponseEntity<Map> response = new RestTemplate().postForEntity(url, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || !response.getBody().containsKey("idToken")) {
            throw new Exception("Échec de génération du token Firebase");
        }

        return buildResponse(user, response.getBody().get("idToken"), customToken);
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

