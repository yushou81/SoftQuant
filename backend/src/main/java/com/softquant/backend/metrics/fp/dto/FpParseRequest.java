package com.softquant.backend.metrics.fp.dto;

import jakarta.validation.constraints.NotBlank;

public record FpParseRequest(
        String projectName,
        String sourceName,
        @NotBlank(message = "sourceType must not be blank")
        String sourceType,
        @NotBlank(message = "xmlContent must not be blank")
        String xmlContent
) {
}
