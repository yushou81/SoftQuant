package com.softquant.backend.metrics.fp.controller;

import com.softquant.backend.metrics.fp.dto.FpCalculateRequest;
import com.softquant.backend.metrics.fp.dto.FpCalculateResponse;
import com.softquant.backend.metrics.fp.dto.FpParseRequest;
import com.softquant.backend.metrics.fp.dto.FpParseResponse;
import com.softquant.backend.metrics.fp.service.FpCalculateService;
import com.softquant.backend.metrics.fp.service.FpParseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics/fp")
public class FpMetricsController {

    private final FpParseService fpParseService;
    private final FpCalculateService fpCalculateService;

    public FpMetricsController(FpParseService fpParseService, FpCalculateService fpCalculateService) {
        this.fpParseService = fpParseService;
        this.fpCalculateService = fpCalculateService;
    }

    @PostMapping("/parse")
    public FpParseResponse parse(@Valid @RequestBody FpParseRequest request) {
        return fpParseService.parse(request);
    }

    @PostMapping("/calculate")
    public FpCalculateResponse calculate(@Valid @RequestBody FpCalculateRequest request) {
        return fpCalculateService.calculate(request);
    }
}
