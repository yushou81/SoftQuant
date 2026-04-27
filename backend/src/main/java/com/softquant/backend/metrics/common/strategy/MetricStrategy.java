package com.softquant.backend.metrics.common.strategy;

import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.AnalysisResponse;

public interface MetricStrategy {

    String metricSet();

    AnalysisResponse analyze(AnalysisRequest request);
}
