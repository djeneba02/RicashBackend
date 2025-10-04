package com.ricash.ricash.Mappe;

import com.ricash.ricash.dto.AdminSimpleDTO;
import com.ricash.ricash.dto.AgentDTO;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface AgentMapper {

    AgentDTO toDto(Agent agent);
    Agent toEntity(AgentDTO agentDTO);

    AdminSimpleDTO toSimpleDto(Admin admin);

    // REMPLACEZ la méthode @Mapping par une méthode default
    default Map<String, Object> toLoginResponse(Agent agent, String idToken, String customToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("userData", buildAgentData(agent));
        response.put("idToken", idToken);
        response.put("customToken", customToken);
        response.put("userId", agent.getId());
        response.put("email", agent.getEmail());
        response.put("role", agent.getRole());
        return response;
    }

    default Map<String, Object> buildAgentData(Agent agent) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nom", agent.getNom());
        userData.put("prenom", agent.getPrenom());
        userData.put("telephone", agent.getTelephone());
        userData.put("identifiant", agent.getIdentifiant());
        return userData;
    }
}