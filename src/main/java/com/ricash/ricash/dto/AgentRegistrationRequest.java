//package com.ricash.ricash.dto;
//
//import com.google.firebase.database.annotations.NotNull;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Size;
//import lombok.Builder;
//import lombok.Data;
//import org.springframework.web.multipart.MultipartFile;
//
//@Data
//@Builder
//public class AgentRegistrationRequest {
//    @NotBlank
//    @Email
//    private String email;
//
//    @NotBlank
//    @Size(min = 6)
//    private String password;
//
//    @NotBlank
//    private String identifiant;
//
//    @NotBlank
//    private String nom;
//
//    @NotBlank
//    private String prenom;
//
//    private String telephone;
//
//    @NotNull
//    private MultipartFile imageRecto;
//
//    @NotNull
//    private MultipartFile imageVerso;
//}
