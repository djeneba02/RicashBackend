package com.ricash.ricash.dto;

import com.google.firebase.database.annotations.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgenceAssignmentRequest {
    @NotNull
    private Long agentId;

    @NotNull
    private Long agenceId;
}