package com.ricash.ricash.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.service.AgentFundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
public class Agentcontroller {


    private final agentRepository agentRepository;
    private final AgentFundService agentFundService;

    public Agentcontroller(agentRepository agentRepository, AgentFundService agentFundService) {
        this.agentRepository = agentRepository;
        this.agentFundService = agentFundService;
    }

    @GetMapping("/{id}/solde")
    public ResponseEntity<Double> getSoldeAgent(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        String cleanToken = token.replace("Bearer ", "");
        Double solde = agentFundService.consulterSoldeAgent(id, cleanToken);
        return ResponseEntity.ok(solde);
    }

    @GetMapping("/me/solde")
    public ResponseEntity<Double> getMonSolde(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(cleanToken);
            String uid = decodedToken.getUid();


            Agent agent = agentRepository.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            return ResponseEntity.ok(agent.getSoldeCaisse());

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token invalide");
        }
    }
}
