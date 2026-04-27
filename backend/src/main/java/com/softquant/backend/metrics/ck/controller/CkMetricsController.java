package com.softquant.backend.metrics.ck.controller;

import com.softquant.backend.metrics.ck.dto.CkAnalysisRequest;
import com.softquant.backend.metrics.ck.dto.CkAnalysisResponse;
import com.softquant.backend.metrics.ck.service.CkMetricsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/ck")
public class CkMetricsController {

    private final CkMetricsService ckMetricsService;

    public CkMetricsController(CkMetricsService ckMetricsService) {
        this.ckMetricsService = ckMetricsService;
    }

    @PostMapping("/analyze")
    public CkAnalysisResponse analyze(@Valid @RequestBody CkAnalysisRequest request) {
        return ckMetricsService.analyze(request);
    }
}
