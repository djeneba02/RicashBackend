package com.ricash.ricash.dto;

import com.google.firebase.database.annotations.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentValidationRequest {
    @NotNull
    private Long agentId;

    private boolean validation;

    private String raison;
}