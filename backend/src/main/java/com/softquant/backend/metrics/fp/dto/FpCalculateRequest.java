package com.softquant.backend.metrics.fp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FpCalculateRequest(
        String projectName,
        @Valid
        @NotEmpty(message = "components must not be empty")
        List<ComponentInput> components,
        @Valid
        List<GscScoreInput> gscScores,
        @Valid
        LanguageInput language
) {

    public record ComponentInput(
            @NotBlank(message = "candidateId must not be blank")
            String candidateId,
            @NotBlank(message = "componentType must not be blank")
            String componentType,
            @NotBlank(message = "name must not be blank")
            String name,
            Integer det,
            Integer ret,
            Integer ftr,
            String complexityLevel
    ) {
    }

    public record GscScoreInput(
            @NotBlank(message = "code must not be blank")
            String code,
            @Min(value = 0, message = "score must be between 0 and 5")
            @Max(value = 5, message = "score must be between 0 and 5")
            Integer score
    ) {
    }

    public record LanguageInput(
            @NotBlank(message = "code must not be blank")
            String code,
            Integer slocPerFp
    ) {
    }
}
