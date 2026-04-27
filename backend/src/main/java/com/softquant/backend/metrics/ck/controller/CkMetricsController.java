package com.softquant.backend.metrics.ck.controller;

import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.AnalysisResponse;
import com.softquant.backend.metrics.ck.service.MetricsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/ck")
public class CkMetricsController {

    private final MetricsService metricsService;

    public CkMetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return metricsService.analyze(request);
    }
}
