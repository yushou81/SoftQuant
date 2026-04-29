package com.softquant.backend.metrics.loc.controller;

import com.softquant.backend.metrics.loc.dto.LocAnalyzeRequest;
import com.softquant.backend.metrics.loc.dto.LocAnalyzeResponse;
import com.softquant.backend.metrics.loc.service.LocAnalyzeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/loc")
public class LocMetricsController {

    private final LocAnalyzeService locAnalyzeService;

    public LocMetricsController(LocAnalyzeService locAnalyzeService) {
        this.locAnalyzeService = locAnalyzeService;
    }

    @PostMapping("/analyze")
    public LocAnalyzeResponse analyze(@Valid @RequestBody LocAnalyzeRequest request) {
        return locAnalyzeService.analyze(request);
    }
}
