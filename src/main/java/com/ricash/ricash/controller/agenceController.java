package com.ricash.ricash.controller;

import com.ricash.ricash.dto.AgenceDTO;
import com.ricash.ricash.service.agenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/agences")
public class agenceController {


    private final agenceService agenceService;

    public agenceController(agenceService agenceService) {
        this.agenceService = agenceService;
    }

    @PostMapping("/create/by-agent")
    public ResponseEntity<?> createAgenceByAgent(@RequestBody AgenceDTO request,
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            System.out.println("Token reçu: " + idToken);

            AgenceDTO created = agenceService.createAgenceByAgent(request, idToken);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            System.err.println("Erreur création: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping
    public ResponseEntity<List<AgenceDTO>> getAllAgences() {
        List<AgenceDTO> agences = agenceService.getAllAgences();
        return ResponseEntity.ok(agences);
    }

    @PatchMapping("/{agenceId}/status")
    public ResponseEntity<?> toggleAgenceStatus(@PathVariable Long agenceId,
                                                @RequestParam boolean isActive,
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            AgenceDTO updatedAgence = agenceService.toggleAgenceStatus(agenceId, isActive, idToken);
            return ResponseEntity.ok(updatedAgence);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
