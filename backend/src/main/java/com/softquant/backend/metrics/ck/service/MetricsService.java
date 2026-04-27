package com.softquant.backend.metrics.ck.service;

import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.AnalysisResponse;
import com.softquant.backend.metrics.common.strategy.MetricStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Map<String, MetricStrategy> strategyMap = new HashMap<>();

    public MetricsService(List<MetricStrategy> strategies) {
        for (MetricStrategy strategy : strategies) {
            strategyMap.put(strategy.metricSet(), strategy);
        }
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        String metricSet = request.getMetricSet() == null || request.getMetricSet().isBlank()
                ? "ck"
                : request.getMetricSet().trim().toLowerCase();
        MetricStrategy strategy = strategyMap.get(metricSet);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported metric set: " + metricSet);
        }
        return strategy.analyze(request);
    }
}
