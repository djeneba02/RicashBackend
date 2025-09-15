package com.ricash.ricash.repository;

import com.ricash.ricash.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface adminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUid(String uid);
    Optional <Admin> findByEmail(String email);
    boolean existsByUid(String uid);
    boolean existsByEmail(String email);
}
