package com.ricash.ricash.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.model.*;
import com.ricash.ricash.repository.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class FirebaseTokendFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokendFilter.class);

    private final userRepository userRepository;
    private final adminRepository adminRepository;
    private final agentRepository agentRepository;

    public FirebaseTokendFilter(userRepository userRepository,
                               adminRepository adminRepository,
                               agentRepository agentRepository) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.agentRepository = agentRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        final String path = request.getRequestURI();
        final String method = request.getMethod();

        logger.debug("Processing request: {} {}", method, path);

        // Endpoints publics - autorisés sans authentification
        if (isPublicEndpoint(path, method)) {
            logger.debug("Public endpoint - skipping authentication");
            chain.doFilter(request, response);
            return;
        }

        // Vérification header Authorization
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header missing or invalid");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token manquant");
            return;
        }

        // Extraction du token
        final String idToken = authHeader.substring(7);

        try {
            // 1. Vérification du token Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            logger.info("Token valide pour UID: {}", decodedToken.getUid());

            // 2. Recherche de l'utilisateur dans les différentes tables
            UserDetailsWrapper userDetails = findUserInAllTables(decodedToken.getUid());

            if (userDetails == null) {
                logger.error("Utilisateur non trouvé pour UID: {}", decodedToken.getUid());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utilisateur non enregistré");
                return;
            }

            // 3. Vérification que l'utilisateur est actif
            if (!userDetails.isActive()) {
                logger.error("Utilisateur inactif: {}", decodedToken.getUid());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Compte désactivé");
                return;
            }

            // 4. Création de l'authentication avec le rôle approprié
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(userDetails.getRole()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication successful for user: {}", userDetails.getEmail());

            chain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            logger.error("Erreur de validation du token Firebase", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide");
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'authentification", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur serveur");
        }
    }

    public UserDetailsWrapper findUserInAllTables(String uid) {
        logger.debug("Recherche de l'utilisateur avec UID: {}", uid);

        // Recherche d'abord dans User (clients)
        Optional<User> userOpt = userRepository.findByUid(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logger.debug("Utilisateur trouvé dans la table User: {}", user.getEmail());
            return new UserDetailsWrapper(
                    user.getId(),
                    uid,
                    user.getEmail(),
                    "ROLE_USER",  // Changé de ROLE_CLIENT à ROLE_USER
                    user.isActif(),
                    "USER"
            );
        }

        // Recherche dans Admin
        Optional<Admin> adminOpt = adminRepository.findByUid(uid);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            logger.debug("Utilisateur trouvé dans la table Admin: {}", admin.getEmail());
            return new UserDetailsWrapper(
                    admin.getId(),
                    uid,
                    admin.getEmail(),
                    "ROLE_ADMIN",
                    admin.isEstActif(),
                    "ADMIN"
            );
        }

        // Recherche dans Agent
        Optional<Agent> agentOpt = agentRepository.findByUid(uid);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            logger.debug("Utilisateur trouvé dans la table Agent: {}", agent.getEmail());
            return new UserDetailsWrapper(
                    agent.getId(),
                    uid,
                    agent.getEmail(),
                    "ROLE_AGENT",
                    agent.isEstActif(),
                    "AGENT"
            );
        }

        logger.warn("Aucun utilisateur trouvé avec UID: {}", uid);
        return null;
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Liste des endpoints publics
        return path.startsWith("/api/auth") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/uploads/") ||
                path.equals("/error") ||
                (path.equals("/api/users/register") && method.equals("POST")) ||
                (path.equals("/api/agents/register") && method.equals("POST")) ||
                (path.equals("/api/admins/register") && method.equals("POST")) ||
                path.startsWith("/public/") ||
                method.equals("OPTIONS"); // Autoriser les requêtes OPTIONS
    }

    // Classe wrapper pour uniformiser les différents types d'utilisateurs
    public static class UserDetailsWrapper {
        private final Long id;
        private final String uid;
        private final String email;
        private final String role; // ROLE_ format Spring Security
        private final boolean active;
        private final String userType; // USER, ADMIN, AGENT

        public UserDetailsWrapper(Long id, String uid, String email, String role, boolean active, String userType) {
            this.id = id;
            this.uid = uid;
            this.email = email;
            this.role = role;
            this.active = active;
            this.userType = userType;
        }

        public Long getId() { return id; }
        public String getUid() { return uid; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isActive() { return active; }
        public String getUserType() { return userType; }

        @Override
        public String toString() {
            return "UserDetailsWrapper{" +
                    "id=" + id +
                    ", email='" + email + '\'' +
                    ", role='" + role + '\'' +
                    ", userType='" + userType + '\'' +
                    '}';
        }
    }


}