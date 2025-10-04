package com.ricash.ricash.service;

import com.ricash.ricash.dto.UserRegistrationRequest;
import com.ricash.ricash.dto.UserResponseDTO;
import com.ricash.ricash.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface userService {
    User registerUser(UserRegistrationRequest userRequest, MultipartFile rectoFile, MultipartFile versoFile) throws Exception;
    User validateUserDocuments(UserRegistrationRequest request, boolean isValid, String token) ;
    User toggleUserStatus(Long userId, boolean isActive, String token);

    List<UserResponseDTO> getAllUsers();
    List<UserResponseDTO> getActiveUsers();
    List<UserResponseDTO> getInactiveUsers();
    List<UserResponseDTO> getUsersByKycStatus(String kycStatus);
    List<UserResponseDTO> getUsersWithActiveKyc();
    List<UserResponseDTO> getUsersWithRejectedKyc();
    List<UserResponseDTO> getUsersWithPendingKyc();
}
