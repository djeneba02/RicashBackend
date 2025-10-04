package com.ricash.ricash.controller;

import com.ricash.ricash.dto.UserResponseDTO;
import com.ricash.ricash.service.userService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final userService userService;

    public UserController(userService userService) {
        this.userService = userService;
    }

    // Récupérer les utilisateurs par statut KYC
    @GetMapping("/kyc-status/{kycStatus}")
    public List<UserResponseDTO> getUsersByKycStatus(@PathVariable String kycStatus) {
        return userService.getUsersByKycStatus(kycStatus);
    }

    // Utilisateurs avec KYC actif (VERIFIE)
    @GetMapping("/kyc-active")
    public List<UserResponseDTO> getUsersWithActiveKyc() {
        return userService.getUsersWithActiveKyc();
    }

    // Utilisateurs avec KYC rejeté (REJETE)
    @GetMapping("/kyc-rejected")
    public List<UserResponseDTO> getUsersWithRejectedKyc() {
        return userService.getUsersWithRejectedKyc();
    }

    // Utilisateurs avec KYC en attente (EN_COURS)
    @GetMapping("/kyc-pending")
    public List<UserResponseDTO> getUsersWithPendingKyc() {
        return userService.getUsersWithPendingKyc();
    }

    // Utilisateurs actifs
    @GetMapping("/active")
    public List<UserResponseDTO> getActiveUsers() {
        return userService.getActiveUsers();
    }

    // Utilisateurs inactifs
    @GetMapping("/inactive")
    public List<UserResponseDTO> getInactiveUsers() {
        return userService.getInactiveUsers();
    }

    // Tous les utilisateurs
    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }
}