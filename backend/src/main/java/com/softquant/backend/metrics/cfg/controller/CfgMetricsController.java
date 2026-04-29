package com.softquant.backend.metrics.cfg.controller;

import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeRequest;
import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeResponse;
import com.softquant.backend.metrics.cfg.service.CfgAnalyzeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/cfg")
public class CfgMetricsController {

    private final CfgAnalyzeService cfgAnalyzeService;

    public CfgMetricsController(CfgAnalyzeService cfgAnalyzeService) {
        this.cfgAnalyzeService = cfgAnalyzeService;
    }

    @PostMapping("/analyze")
    public CfgAnalyzeResponse analyze(@Valid @RequestBody CfgAnalyzeRequest request) {
        return cfgAnalyzeService.analyze(request);
    }
}
