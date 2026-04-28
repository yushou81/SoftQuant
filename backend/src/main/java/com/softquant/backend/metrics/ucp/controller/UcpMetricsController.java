package com.softquant.backend.metrics.ucp.controller;

import com.softquant.backend.metrics.ucp.dto.UcpCalculateRequest;
import com.softquant.backend.metrics.ucp.dto.UcpCalculateResponse;
import com.softquant.backend.metrics.ucp.dto.UcpParseRequest;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
import com.softquant.backend.metrics.ucp.service.UcpCalculateService;
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
    private final UcpCalculateService ucpCalculateService;

    public UcpMetricsController(UcpParseService ucpParseService, UcpCalculateService ucpCalculateService) {
        this.ucpParseService = ucpParseService;
        this.ucpCalculateService = ucpCalculateService;
    }

    @PostMapping("/parse")
    public UcpParseResponse parse(@Valid @RequestBody UcpParseRequest request) {
        return ucpParseService.parse(request);
    }

    @PostMapping("/calculate")
    public UcpCalculateResponse calculate(@Valid @RequestBody UcpCalculateRequest request) {
        return ucpCalculateService.calculate(request);
    }
}
