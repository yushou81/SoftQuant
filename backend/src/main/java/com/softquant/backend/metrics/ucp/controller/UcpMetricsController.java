package com.softquant.backend.metrics.ucp.controller;

import com.softquant.backend.metrics.ucp.dto.UcpParseRequest;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
import com.softquant.backend.metrics.ucp.service.UcpParseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/ucp")
public class UcpMetricsController {

    private final UcpParseService ucpParseService;

    public UcpMetricsController(UcpParseService ucpParseService) {
        this.ucpParseService = ucpParseService;
    }

    @PostMapping("/parse")
    public UcpParseResponse parse(@Valid @RequestBody UcpParseRequest request) {
        return ucpParseService.parse(request);
    }
}
