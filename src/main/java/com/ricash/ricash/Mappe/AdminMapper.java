package com.ricash.ricash.Mappe;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.ricash.ricash.model.Admin;
import com.ricash.ricash.dto.AdminSimpleDTO;
import com.ricash.ricash.dto.UserResponseDTO;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    // Mapper Admin vers AdminSimpleDTO (similaire à toSimpleDto dans AgentMapper)
    AdminSimpleDTO toDto(Admin admin);
    UserResponseDTO toUserResponseDto(Admin admin);

    // Méthode pour la réponse de login (similaire à toLoginResponse dans AgentMapper)
    default Map<String, Object> toLoginResponse(Admin admin, String idToken, String customToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("userData", buildAdminData(admin));
        response.put("idToken", idToken);
        response.put("customToken", customToken);
        response.put("userId", admin.getId());
        response.put("email", admin.getEmail());
        response.put("role", admin.getRole());
        return response;
    }

    default Map<String, Object> buildAdminData(Admin admin) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nom", admin.getNom());
        userData.put("prenom", admin.getPrenom());
        userData.put("telephone", admin.getTelephone());
        userData.put("email", admin.getEmail());
        userData.put("role", admin.getRole());
        return userData;
    }
}