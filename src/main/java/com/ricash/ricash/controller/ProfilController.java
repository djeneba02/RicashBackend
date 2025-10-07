package com.ricash.ricash.controller;

import com.ricash.ricash.service.ProfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profil")
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    // Upload / modifier avatar agent
    @PostMapping("/agent/{id}/avatar")
    public ResponseEntity<String> uploadAvatarAgent(@PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        String url = profilService.uploadAvatarAgent(id, file);
        return ResponseEntity.ok(url);
    }

    // Supprimer avatar agent
    @DeleteMapping("/agent/{id}/avatar")
    public ResponseEntity<Void> deleteAvatarAgent(@PathVariable Long id) {
        profilService.deleteAvatarAgent(id);
        return ResponseEntity.noContent().build();
    }

    // Upload / modifier avatar admin
    @PostMapping("/admin/{id}/avatar")
    public ResponseEntity<String> uploadAvatarAdmin(@PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        String url = profilService.uploadAvatarAdmin(id, file);
        return ResponseEntity.ok(url);
    }

    // Supprimer avatar admin
    @DeleteMapping("/admin/{id}/avatar")
    public ResponseEntity<Void> deleteAvatarAdmin(@PathVariable Long id) {
        profilService.deleteAvatarAdmin(id);
        return ResponseEntity.noContent().build();
    }


}
