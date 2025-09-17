package com.ricash.ricash.controller;

import com.ricash.ricash.model.Agent;
import com.ricash.ricash.service.AgentFundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent-funds")
public class AgentFundController {

    private final AgentFundService agentFundService;

    public AgentFundController(AgentFundService agentFundService) {
        this.agentFundService = agentFundService;
    }

    @PostMapping("/approvisionner/{agentId}")
    public ResponseEntity<?> approvisionnerCaisse(
            @PathVariable Long agentId,
            @RequestParam Double montant,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            System.out.println("Token reçu: " + idToken);

            Agent agent = agentFundService.approvisionnerCaisseAgent(agentId, montant, idToken);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            System.err.println("Erreur création: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/retirer/{agentId}")
    public ResponseEntity<?> retirerCaisse(
            @PathVariable Long agentId,
            @RequestParam Double montant,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            System.out.println("Token reçu: " + idToken);

            Agent agent = agentFundService.retirerCaisseAgent(agentId, montant, idToken);
            return ResponseEntity.ok(agent);
        } catch (RuntimeException e) {
            System.err.println("Erreur création: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/solde/{agentId}")
    public ResponseEntity<?> consulterSolde(
            @PathVariable Long agentId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String idToken = authHeader.substring("Bearer ".length()).trim();
            System.out.println("Token reçu: " + idToken);

            Double solde = agentFundService.consulterSoldeAgent(agentId, idToken);
            return ResponseEntity.ok(solde);
        } catch (RuntimeException e) {
            System.err.println("Erreur création: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}