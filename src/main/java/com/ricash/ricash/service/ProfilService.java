package com.ricash.ricash.service;

import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProfilService {

    private final agentRepository agentRepository;
    private final adminRepository adminRepository;

    public ProfilService(agentRepository agentRepository, adminRepository adminRepository) {
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
    }

    // Méthode pour uploader un fichier localement
    private String uploadFile(MultipartFile file, String subDirectory) throws IOException {
        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "uploads/" + subDirectory + "/";
                String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Construction de l'URL complète
                String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/")
                        .path(subDirectory)
                        .path("/")
                        .path(filename)
                        .toUriString();

                return fileUri;
            } catch (IOException e) {
                throw new RuntimeException("Échec de l'upload du fichier", e);
            }
        }
        throw new RuntimeException("Fichier vide");
    }

    // Méthode pour supprimer un fichier local
    private void deleteFile(String fileUrl, String subDirectory) {
        if (fileUrl != null) {
            try {
                // Extraire le nom du fichier depuis l'URL
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get("uploads/" + subDirectory + "/" + filename);

                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                // Log l'erreur mais ne pas bloquer la suppression en base de données
                System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
            }
        }
    }

    // Ajouter ou modifier l'avatar Agent
    public String uploadAvatarAgent(Long agentId, MultipartFile file) throws IOException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        if (file != null && !file.isEmpty()) {
            String avatarUrl = uploadFile(file, "avatars/agents");
            agent.setAvatarUrl(avatarUrl);
            agent.setUpdatedAt(LocalDateTime.now());
            agentRepository.save(agent);
            return avatarUrl;
        }
        throw new RuntimeException("Fichier vide");
    }

    // Ajouter ou modifier l'avatar Admin
    public String uploadAvatarAdmin(Long adminId, MultipartFile file) throws IOException {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        if (file != null && !file.isEmpty()) {
            String avatarUrl = uploadFile(file, "avatars/admins");
            admin.setAvatarUrl(avatarUrl);
            admin.setUpdatedAt(LocalDateTime.now());
            adminRepository.save(admin);
            return avatarUrl;
        }
        throw new RuntimeException("Fichier vide");
    }

    // Supprimer l'avatar Agent
    public void deleteAvatarAgent(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        // Supprimer le fichier physique
        deleteFile(agent.getAvatarUrl(), "avatars/agents");

        // Supprimer l'URL dans la base de données
        agent.setAvatarUrl(null);
        agent.setUpdatedAt(LocalDateTime.now());
        agentRepository.save(agent);
    }

    // Supprimer l'avatar Admin
    public void deleteAvatarAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Supprimer le fichier physique
        deleteFile(admin.getAvatarUrl(), "avatars/admins");

        // Supprimer l'URL dans la base de données
        admin.setAvatarUrl(null);
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);
    }
}