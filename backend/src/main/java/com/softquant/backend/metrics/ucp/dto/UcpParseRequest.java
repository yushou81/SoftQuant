package com.softquant.backend.metrics.ucp.dto;

import jakarta.validation.constraints.NotBlank;

public record UcpParseRequest(
        String projectName,
        String sourceName,
        @NotBlank(message = "sourceType must not be blank")
        String sourceType,
        @NotBlank(message = "xmlContent must not be blank")
        String xmlContent
) {
}
