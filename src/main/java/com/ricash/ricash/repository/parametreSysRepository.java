package com.ricash.ricash.repository;

import com.ricash.ricash.model.ParametreSysteme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface parametreSysRepository extends JpaRepository<ParametreSysteme, Long> {
    Optional<ParametreSysteme> findFirstByOrderByIdDesc();
}