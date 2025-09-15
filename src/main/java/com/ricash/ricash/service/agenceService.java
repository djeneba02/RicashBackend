package com.ricash.ricash.service;

import com.ricash.ricash.dto.AgenceDTO;
import com.ricash.ricash.model.Agence;

import java.util.List;

public interface agenceService {
    AgenceDTO createAgenceByAgent(AgenceDTO request, String token);
    List<AgenceDTO> getAllAgences();
    AgenceDTO toggleAgenceStatus(Long agenceId, boolean isActive, String token);
}
