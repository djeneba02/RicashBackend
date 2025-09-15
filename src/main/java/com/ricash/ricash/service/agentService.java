package com.ricash.ricash.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.ricash.ricash.dto.AgentValidationRequest;
import com.ricash.ricash.model.Agent;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface agentService {

    Agent registerAgent(Agent request, MultipartFile file, MultipartFile fil)throws FirebaseAuthException, IOException;
    List<Agent> getAgentsEnAttente();
    List<Agent> getAgentsValides();
    Agent validateAgent(AgentValidationRequest request, String token);
    Agent toggleAgentStatus(Long agentId, boolean isActive, String token);
    List<Agent> getAllAgents();


}
