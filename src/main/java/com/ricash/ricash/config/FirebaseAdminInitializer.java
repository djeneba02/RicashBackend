package com.ricash.ricash.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.ricash.ricash.model.Enum.statutKYC;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.repository.adminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FirebaseAdminInitializer {

    private final FirebaseAuth firebaseAuth;
    private final adminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public FirebaseAdminInitializer(FirebaseAuth firebaseAuth,
                            adminRepository adminRepository,
                            PasswordEncoder passwordEncoder) {
        this.firebaseAuth = firebaseAuth;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initAdmin() {
        try {
            String adminEmail = "admin@ricash.com";
            String adminPassword = "AdminRicash123@";
            String encodedPassword = passwordEncoder.encode(adminPassword);

            // Vérifier si l'admin existe déjà dans la base de données
            Optional<Admin> existingAdmin = adminRepository.findByEmail(adminEmail);
            if (existingAdmin.isPresent()) {
                System.out.println("✅ Admin existe déjà dans la base de données");
                return;
            }

            // Vérifier si l'admin existe dans Firebase
            UserRecord firebaseUser = null;
            try {
                firebaseUser = firebaseAuth.getUserByEmail(adminEmail);
                System.out.println("✅ Admin existe déjà dans Firebase");
            } catch (FirebaseAuthException e) {
                // L'utilisateur n'existe pas, nous allons le créer
                System.out.println("ℹ️ Création de l'admin dans Firebase...");
            }

            // Créer l'utilisateur dans Firebase s'il n'existe pas
            if (firebaseUser == null) {
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(adminEmail)
                        .setPassword(adminPassword)
                        .setDisplayName("Admin System");

                firebaseUser = firebaseAuth.createUser(request);
                System.out.println("✅ Admin créé dans Firebase avec UID: " + firebaseUser.getUid());
            }

            // Créer l'admin dans la base de données MySQL
            Admin admin = new Admin();
            admin.setUid(firebaseUser.getUid());
            admin.setNom("Admin");
            admin.setPrenom("System");
            admin.setEmail(adminEmail);
            admin.setTelephone("+22177546634");
            admin.setKycStatut(statutKYC.VERIFIE);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setMotDePasse(encodedPassword);
            admin.setEstActif(true);

            adminRepository.save(admin);
            System.out.println("✅ Admin créé avec succès dans la base de données");
            System.out.println("🔑 Email: " + adminEmail);
            System.out.println("🔑 Mot de passe: " + adminPassword);

        } catch (FirebaseAuthException e) {
            System.err.println("❌ Erreur Firebase lors de la création de l'admin: " + e.getMessage());
            createAdminWithoutFirebase();
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue lors de la création de l'admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createAdminWithoutFirebase() {
        try {
            String adminEmail = "admin@ricash.com";

            // Vérifier si l'admin existe déjà
            if (adminRepository.findByEmail(adminEmail).isPresent()) {
                System.out.println("✅ Admin existe déjà (sans Firebase)");
                return;
            }

            // Créer l'admin sans UID Firebase
            Admin admin = new Admin();
            admin.setNom("Admin");
            admin.setPrenom("System");
            admin.setEmail(adminEmail);
            admin.setTelephone("+22177546634");
            admin.setKycStatut(statutKYC.VERIFIE);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            adminRepository.save(admin);
            System.out.println("⚠️ Admin créé sans UID Firebase (mode de secours)");
            System.out.println("🔑 Email: " + adminEmail);
            System.out.println("⚠️ Note: L'authentification Firebase ne fonctionnera pas pour cet admin");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de l'admin sans Firebase: " + e.getMessage());
        }
    }
}