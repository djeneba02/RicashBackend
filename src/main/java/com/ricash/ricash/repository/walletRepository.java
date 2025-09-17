package com.ricash.ricash.repository;

import com.ricash.ricash.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface walletRepository extends JpaRepository<Wallet, Long> {

}