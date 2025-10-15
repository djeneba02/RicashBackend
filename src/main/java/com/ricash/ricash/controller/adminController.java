package com.ricash.ricash.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.ricash.ricash.dto.AgentDTO;
import com.ricash.ricash.dto.AgentValidationRequest;
import com.ricash.ricash.dto.UserRegistrationRequest;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.Enum.statutKYC;
import com.ricash.ricash.model.User;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.repository.userRepository;
import com.ricash.ricash.service.agenceService;
import com.ricash.ricash.service.agentService;
import com.ricash.ricash.service.userService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class adminController {

    private final agentService agentService;
    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final userService userService;
//    private final agenceService agenceService;

    public adminController(agentService agentService, userRepository userRepository, agentRepository agentRepository, userService userService) {
        this.agentService = agentService;
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.userService = userService;
    }

    @GetMapping("/agents/en-attente")
    public ResponseEntity<List<AgentDTO>> getAgentsEnAttente() {
        try {
            List<AgentDTO> agents = agentService.getAgentsEnAttente();
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Valider ou rejeter un agent
    @PostMapping("/agents/validation")
    public ResponseEntity<?> validateAgent(@RequestBody AgentValidationRequest request,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            System.out.println("Token reçu: " + idToken);

            AgentDTO validatedAgent = agentService.validateAgent(request, idToken);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Agent " + (request.isValidation() ? "validé" : "rejeté") + " avec succès");
            response.put("agent", validatedAgent);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("Erreur validation: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur de validation", "message", e.getMessage())
            );
        }
    }

    // Valider ou rejeter les documents d'un utilisateur
    @PostMapping("/users/validation-documents")
    public ResponseEntity<?> validateUserDocuments(@RequestBody UserRegistrationRequest request,
                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            // Extraire le token du header Authorization
            String token = authHeader.replace("Bearer ", "").trim();

            // Déterminer si c'est une validation ou un rejet
            boolean isValid = request.getRaison() == null || request.getRaison().isEmpty();

            // Appeler le service pour valider les documents
            User validatedUser = userService.validateUserDocuments(request, isValid, token);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Documents utilisateur " + (isValid ? "validés" : "rejetés") + " avec succès");
            response.put("userId", validatedUser.getId());
            response.put("isActive", validatedUser.isActif());
            response.put("kycStatus", validatedUser.getKycStatut());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Erreur validation documents: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur de validation", "message", e.getMessage())
            );
        } catch (Exception e) {
            System.err.println("Erreur serveur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Erreur serveur", "message", e.getMessage())
            );
        }
    }


    // Récupérer un agent par son ID
    @GetMapping("/agents/{id}")
    public ResponseEntity<AgentDTO> getAgentById(@PathVariable Long id) {
        try {
            AgentDTO agent = agentService.getAgentById(id);
            if (agent != null) {
                return ResponseEntity.ok(agent);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/agents/{agentId}/toggle-status")
    public ResponseEntity<?> toggleAgentStatus(@PathVariable Long agentId,
                                               @RequestParam boolean active,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "").trim();
            AgentDTO updatedAgent = agentService.toggleAgentStatus(agentId, active, token);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statut de l'agent modifié avec succès");
            response.put("agent", updatedAgent);
            response.put("estActif", updatedAgent.isEstActif());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur de modification", "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Erreur serveur", "message", e.getMessage())
            );
        }
    }


    // Récupérer tous les agents
    @GetMapping("/agents")
    public ResponseEntity<List<AgentDTO>> getAllAgents() {
        try {
            List<AgentDTO> agents = agentService.getAllAgents();
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Récupérer les agents actifs
    @GetMapping("/agents/actifs")
    public ResponseEntity<List<AgentDTO>> getAgentsActifs() {
        try {
            List<AgentDTO> agents = agentService.getAgentsValides().stream()
                    .filter(AgentDTO::isEstActif)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/agents/inactifs")
    public ResponseEntity<List<AgentDTO>> getAgentsInactifs() {
        try {
            List<AgentDTO> agents = agentService.getAllAgents().stream()
                    .filter(agent -> !agent.isEstActif())
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/users/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId,
                                              @RequestParam boolean active,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            // Extraire le token du header Authorization
            String token = authHeader.replace("Bearer ", "").trim();

            User updatedUser = userService.toggleUserStatus(userId, active, token);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statut de l'utilisateur modifié avec succès");
            response.put("userId", updatedUser.getId());
            response.put("email", updatedUser.getEmail());
            response.put("isActive", updatedUser.isActif());
            response.put("updatedBy", updatedUser.getAdmin() != null ? updatedUser.getAdmin().getEmail() : "N/A");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur de modification", "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Erreur serveur", "message", e.getMessage())
            );
        }
    }

    // Récupérer tous les utilisateurs
//    @GetMapping("/users")
//    public ResponseEntity<List<User>> getAllUsers() {
//        try {
//            List<User> users = userService.getAllUsers();
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

//    // Récupérer les utilisateurs actifs
//    @GetMapping("/users/actifs")
//    public ResponseEntity<List<User>> getUsersActifs() {
//        try {
//            List<User> users = userService.getActiveUsers();
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    // Récupérer les utilisateurs inactifs
//    @GetMapping("/users/inactifs")
//    public ResponseEntity<List<User>> getUsersInactifs() {
//        try {
//            List<User> users = userService.getInactiveUsers();
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

    // Récupérer les utilisateurs en attente de validation KYC
//    @GetMapping("/users/en-attente-validation")
//    public ResponseEntity<List<User>> getUsersEnAttenteValidation() {
//        try {
//            List<User> users = userRepository.findByActifFalseAndDocumentsIdentiteEstVerifieFalse();
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

    // Récupérer les utilisateurs avec KYC validé
    @GetMapping("/users/kyc-valides")
    public ResponseEntity<List<User>> getUsersKycValides() {
        try {
            List<User> users = userRepository.findByKycStatut(statutKYC.VERIFIE);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Récupérer les utilisateurs avec KYC rejeté
    @GetMapping("/users/kyc-rejetes")
    public ResponseEntity<List<User>> getUsersKycRejetes() {
        try {
            List<User> users = userRepository.findByKycStatut(statutKYC.REJETE);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
