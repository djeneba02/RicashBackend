package com.ricash.ricash.controller;

import com.ricash.ricash.dto.DemandeFondsRequest;
import com.ricash.ricash.dto.TraitementDemandeRequest;
import com.ricash.ricash.model.DemandeFonds;
import com.ricash.ricash.service.DemandeFondsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-fonds")
public class DemandeFondsController {

    private final DemandeFondsService demandeFondsService;

    public DemandeFondsController(DemandeFondsService demandeFondsService) {
        this.demandeFondsService = demandeFondsService;
    }

    // Agent crée une demande
    @PostMapping("/creer")
    public ResponseEntity<DemandeFonds> creerDemande(
            @RequestBody DemandeFondsRequest request,
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        DemandeFonds demande = demandeFondsService.creerDemandeFonds(
                request.getMontant(),
                request.getMotif(),
                cleanToken
        );
        return ResponseEntity.ok(demande);
    }

    // Admin approuve une demande
    @PostMapping("/approuver")
    public ResponseEntity<DemandeFonds> approuverDemande(
            @RequestBody TraitementDemandeRequest request,
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        DemandeFonds demande = demandeFondsService.approuverDemande(
                request.getDemandeId(),
                cleanToken
        );
        return ResponseEntity.ok(demande);
    }

    // Admin rejette une demande
    @PostMapping("/rejeter")
    public ResponseEntity<DemandeFonds> rejeterDemande(
            @RequestBody TraitementDemandeRequest request,
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        DemandeFonds demande = demandeFondsService.rejeterDemande(
                request.getDemandeId(),
                request.getRaison(),
                cleanToken
        );
        return ResponseEntity.ok(demande);
    }

    // Agent voit ses demandes
    @GetMapping("/mes-demandes")
    public ResponseEntity<List<DemandeFonds>> getMesDemandes(
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        List<DemandeFonds> demandes = demandeFondsService.getMesDemandes(cleanToken);
        return ResponseEntity.ok(demandes);
    }

    // Admin voit les demandes en attente
    @GetMapping("/en-attente")
    public ResponseEntity<List<DemandeFonds>> getDemandesEnAttente(
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        List<DemandeFonds> demandes = demandeFondsService.getDemandesEnAttente(cleanToken);
        return ResponseEntity.ok(demandes);
    }
}