package com.softquant.backend.metrics.loc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LocAnalyzeRequest(
        String projectName,
        @Valid
        @NotEmpty(message = "sources must not be empty")
        List<SourceInput> sources
) {

    public record SourceInput(
            @NotBlank(message = "fileName must not be blank")
            String fileName,
            String language,
            @NotNull(message = "content must not be null")
            String content
    ) {
    }
}
