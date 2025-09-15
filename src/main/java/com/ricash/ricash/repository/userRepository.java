package com.ricash.ricash.repository;

import com.ricash.ricash.model.Enum.statutKYC;
import com.ricash.ricash.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface userRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String uid);
    Optional <User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByActifTrue();
    List<User> findByActifFalse();
    List<User> findByKycStatut(statutKYC kycStatut);
    Optional<User> findByTelephone(String telephone);
    List<User> findByActifTrueAndKycStatut(statutKYC kycStatut);
    List<User> findByActifFalseAndKycStatut(statutKYC kycStatut);
    boolean existsByDocumentsIdentiteNumero(String identifiant);
    boolean existsByUid(String uid);

}
