package com.softquant.backend.metrics.cfg.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CfgAnalyzeRequest(
        String projectName,
        @Valid
        @NotEmpty(message = "sources must not be empty")
        List<SourceInput> sources
) {

    public record SourceInput(
            @NotBlank(message = "fileName must not be blank")
            String fileName,
            @NotBlank(message = "content must not be blank")
            String content
    ) {
    }
}
