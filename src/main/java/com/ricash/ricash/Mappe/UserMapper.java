//package com.ricash.ricash.Mappe;
//
//import com.ricash.ricash.dto.*;
//import com.ricash.ricash.model.*;
//import org.springframework.stereotype.Component;
//
//import java.util.stream.Collectors;
//
//@Component
//public class UserMapper {
//
//    public UserResponseDTO toDTO(User user) {
//        if (user == null) {
//            return null;
//        }
//
//        UserResponseDTO dto = new UserResponseDTO();
//        dto.setId(user.getId());
//        dto.setUid(user.getUid());
//        dto.setNom(user.getNom());
//        dto.setPrenom(user.getPrenom());
//        dto.setEmail(user.getEmail());
//        dto.setTelephone(user.getTelephone());
//        dto.setDateCreation(user.getDateCreation());
//        dto.setDateNaissance(user.getDateNaissance());
//        dto.setKycStatut(user.getKycStatut());
//        dto.setActif(user.isActif());
//        dto.setRole(user.getRole());
//        dto.setRaisonRejet(user.getRaisonRejet());
//
//        // Mapper l'adresse
//        if (user.getAdresse() != null) {
//            dto.setAdresse(toAdresseDTO(user.getAdresse()));
//        }
//
//        // Mapper le wallet
//        if (user.getPortefeuille() != null) {
//            dto.setPortefeuille(toWalletDTO(user.getPortefeuille()));
//        }
//
//        // Mapper les documents d'identité
//        if (user.getDocumentsIdentite() != null) {
//            dto.setDocumentsIdentite(user.getDocumentsIdentite().stream()
//                    .map(this::toDocumentIdentiteDTO)
//                    .collect(Collectors.toList()));
//        }
//
//        // Mapper l'admin (seulement les infos basiques)
//        if (user.getAdmin() != null) {
//            dto.setAdmin(toAdminSimpleDTO(user.getAdmin()));
//        }
//
//        return dto;
//    }
//
//    private AdresseDTO toAdresseDTO(Adresse adresse) {
//        AdresseDTO dto = new AdresseDTO();
//        dto.setLigne1(adresse.getLigne1());
//        dto.setLigne2(adresse.getLigne2());
//        dto.setVille(adresse.getVille());
//        dto.setCodePostal(adresse.getCodePostal());
//        dto.setPays(adresse.getPays());
//        return dto;
//    }
//
//    private WalletDTO toWalletDTO(Wallet wallet) {
//        WalletDTO dto = new WalletDTO();
//        dto.setId(wallet.getId());
//        dto.setSolde(wallet.getSolde());
//        dto.setDevise(wallet.getDevise());
//        dto.setDateDerniereMAJ(wallet.getDateDerniereMAJ());
//        return dto;
//    }
//
//    private DocumentIdentiteDTO toDocumentIdentiteDTO(DocumentIdentite document) {
//        DocumentIdentiteDTO dto = new DocumentIdentiteDTO();
//        dto.setId(document.getId());
//        dto.setType(document.getType().name());
//        dto.setNumero(document.getNumero());
//        dto.setImageRectoUrl(document.getImageRectoUrl());
//        dto.setImageVersoUrl(document.getImageVersoUrl());
//        dto.setDateValidation(document.getDateValidation());
//        dto.setDateExpiration(document.getDateExpiration());
//        return dto;
//    }
//
//    private AdminSimpleDTO toAdminSimpleDTO(Admin admin) {
//        AdminSimpleDTO dto = new AdminSimpleDTO();
//        dto.setId(admin.getId());
//        dto.setNom(admin.getNom());
//        dto.setPrenom(admin.getPrenom());
//        dto.setEmail(admin.getEmail());
//        return dto;
//    }
//}


package com.ricash.ricash.Mappe;

import com.ricash.ricash.dto.AdminSimpleDTO;
import com.ricash.ricash.dto.AgentDTO;
import com.ricash.ricash.dto.UserResponseDTO;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toDto(User user);

    default Map<String, Object> toLoginResponse(User user, String idToken, String customToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("userData", buildUserData(user));
        response.put("idToken", idToken);
        response.put("customToken", customToken);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return response;
    }

    default Map<String, Object> buildUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nom", user.getNom());
        userData.put("prenom", user.getPrenom());
        userData.put("telephone", user.getTelephone());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRole());
        return userData;
    }
}