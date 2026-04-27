package com.softquant.backend.metrics.ucp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record UcpCalculateRequest(
        String projectName,
        @Valid
        @NotEmpty(message = "actors must not be empty")
        List<ActorInput> actors,
        @Valid
        @NotEmpty(message = "useCases must not be empty")
        List<UseCaseInput> useCases,
        @Valid
        List<FactorScoreInput> technicalFactors,
        @Valid
        List<FactorScoreInput> environmentalFactors,
        BigDecimal productivity,
        String productivityUnit
) {

    public record ActorInput(
            @NotBlank(message = "actorId must not be blank")
            String actorId,
            @NotBlank(message = "actorName must not be blank")
            String actorName,
            String selectedComplexity,
            Integer selectedWeight
    ) {
    }

    public record UseCaseInput(
            @NotBlank(message = "useCaseId must not be blank")
            String useCaseId,
            @NotBlank(message = "useCaseName must not be blank")
            String useCaseName,
            String selectedComplexity,
            Integer selectedWeight,
            Integer entityCount,
            Integer stepCount,
            Integer classCount
    ) {
    }

    public record FactorScoreInput(
            @NotBlank(message = "code must not be blank")
            String code,
            @Min(value = 0, message = "score must be between 0 and 5")
            @Max(value = 5, message = "score must be between 0 and 5")
            Integer score
    ) {
    }
}
