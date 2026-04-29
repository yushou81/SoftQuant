package com.softquant.backend.metrics.cfg.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeRequest;
import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeResponse;
import com.softquant.backend.metrics.cfg.dto.ClassComplexitySummary;
import com.softquant.backend.metrics.cfg.dto.DecisionPoint;
import com.softquant.backend.metrics.cfg.dto.MethodComplexityMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CfgAnalyzeService {

    private final CyclomaticComplexityService complexityService;
    private final ControlFlowGraphBuilder graphBuilder;

    public CfgAnalyzeService(
            CyclomaticComplexityService complexityService,
            ControlFlowGraphBuilder graphBuilder
    ) {
        this.complexityService = complexityService;
        this.graphBuilder = graphBuilder;
    }

    public CfgAnalyzeResponse analyze(CfgAnalyzeRequest request) {
        List<MethodComplexityMetrics> methods = new ArrayList<>();
        List<String> parseIssues = new ArrayList<>();

        for (CfgAnalyzeRequest.SourceInput source : request.sources()) {
            try {
                var unit = StaticJavaParser.parse(source.content());
                unit.findAll(MethodDeclaration.class)
                        .forEach(method -> methods.add(analyzeMethod(source.fileName(), method)));
                unit.findAll(ConstructorDeclaration.class)
                        .forEach(constructor -> methods.add(analyzeConstructor(source.fileName(), constructor)));
            } catch (RuntimeException ex) {
                parseIssues.add(source.fileName() + ": " + firstLine(ex.getMessage()));
            }
        }

        methods.sort(Comparator.comparing(MethodComplexityMetrics::getClassName)
                .thenComparing(MethodComplexityMetrics::getStartLine)
                .thenComparing(MethodComplexityMetrics::getMethodName));

        CfgAnalyzeResponse response = new CfgAnalyzeResponse();
        response.setModule("CFG");
        response.setContractKind("ANALYSIS");
        response.setProjectName(normalizeProjectName(request.projectName()));
        response.setFileCount(request.sources().size());
        response.setMethodCount(methods.size());
        response.setMaxComplexity(methods.stream().mapToInt(MethodComplexityMetrics::getComplexity).max().orElse(0));
        response.setAvgComplexity(avg(methods.stream().mapToInt(MethodComplexityMetrics::getComplexity).sum(), methods.size()));
        response.setLowRiskMethodCount(countRisk(methods, "LOW"));
        response.setMediumRiskMethodCount(countRisk(methods, "MEDIUM"));
        response.setHighRiskMethodCount(countRisk(methods, "HIGH"));
        response.setVeryHighRiskMethodCount(countRisk(methods, "VERY_HIGH"));
        response.setMethods(methods);
        response.setClassSummaries(buildClassSummaries(methods));
        response.setClassCount(response.getClassSummaries().size());
        response.setParseIssues(parseIssues);
        response.setFormulaTrace(List.of(
                new CfgAnalyzeResponse.TraceLine(
                        "McCabe",
                        "V(G) = E - N + 2P",
                        "Equivalent implementation: 1 + decisionCount"
                ),
                new CfgAnalyzeResponse.TraceLine(
                        "Decision Count",
                        "if/loop/case/catch/?:/&&/|| each contributes 1",
                        String.valueOf(methods.stream().mapToInt(MethodComplexityMetrics::getDecisionPointCount).sum())
                ),
                new CfgAnalyzeResponse.TraceLine(
                        "Risk Threshold",
                        "LOW 1-5, MEDIUM 6-10, HIGH 11-20, VERY_HIGH 21+",
                        response.getHighRiskMethodCount() + response.getVeryHighRiskMethodCount() + " high-risk methods"
                )
        ));
        return response;
    }

    private MethodComplexityMetrics analyzeMethod(String fileName, MethodDeclaration method) {
        CyclomaticComplexityService.ComplexityScan scan = complexityService.scan(method);
        MethodComplexityMetrics metrics = baseMetrics(fileName, method.getNameAsString(), scan);
        metrics.setClassName(classNameOf(method));
        metrics.setSignature(method.getDeclarationAsString(false, false, false));
        metrics.setMethodType("METHOD");
        metrics.setStartLine(method.getRange().map(range -> range.begin.line).orElse(0));
        metrics.setEndLine(method.getRange().map(range -> range.end.line).orElse(0));
        return metrics;
    }

    private MethodComplexityMetrics analyzeConstructor(String fileName, ConstructorDeclaration constructor) {
        CyclomaticComplexityService.ComplexityScan scan = complexityService.scan(constructor);
        MethodComplexityMetrics metrics = baseMetrics(fileName, constructor.getNameAsString(), scan);
        metrics.setClassName(classNameOf(constructor));
        metrics.setSignature(constructor.getDeclarationAsString(false, false, false));
        metrics.setMethodType("CONSTRUCTOR");
        metrics.setStartLine(constructor.getRange().map(range -> range.begin.line).orElse(0));
        metrics.setEndLine(constructor.getRange().map(range -> range.end.line).orElse(0));
        return metrics;
    }

    private MethodComplexityMetrics baseMetrics(
            String fileName,
            String methodName,
            CyclomaticComplexityService.ComplexityScan scan
    ) {
        List<DecisionPoint> decisionPoints = scan.decisionPoints();
        MethodComplexityMetrics metrics = new MethodComplexityMetrics();
        metrics.setFileName(fileName);
        metrics.setMethodName(methodName);
        metrics.setComplexity(scan.complexity());
        metrics.setRiskLevel(complexityService.riskLevel(scan.complexity()));
        metrics.setDecisionPointCount(decisionPoints.size());
        metrics.setDecisionPoints(decisionPoints);
        metrics.setGraph(graphBuilder.build(decisionPoints));
        return metrics;
    }

    private String classNameOf(com.github.javaparser.ast.Node node) {
        return node.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("UnknownClass");
    }

    private List<ClassComplexitySummary> buildClassSummaries(List<MethodComplexityMetrics> methods) {
        Map<String, ClassAccumulator> accumulators = new LinkedHashMap<>();
        for (MethodComplexityMetrics method : methods) {
            String key = method.getFileName() + "|" + method.getClassName();
            accumulators.computeIfAbsent(key, ignored -> new ClassAccumulator(method.getFileName(), method.getClassName()))
                    .add(method);
        }

        return accumulators.values().stream()
                .map(ClassAccumulator::toSummary)
                .sorted(Comparator.comparing(ClassComplexitySummary::getClassName)
                        .thenComparing(ClassComplexitySummary::getFileName))
                .toList();
    }

    private int countRisk(List<MethodComplexityMetrics> methods, String riskLevel) {
        return (int) methods.stream()
                .filter(method -> riskLevel.equals(method.getRiskLevel()))
                .count();
    }

    private BigDecimal avg(int total, int size) {
        if (size == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf((double) total / size).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return "unnamed-project";
        }
        return projectName.trim();
    }

    private String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "parse failed";
        }
        return message.lines().findFirst().orElse("parse failed");
    }

    private final class ClassAccumulator {
        private final String fileName;
        private final String className;
        private int methodCount;
        private int totalComplexity;
        private int maxComplexity;
        private int highRiskMethodCount;

        private ClassAccumulator(String fileName, String className) {
            this.fileName = fileName;
            this.className = className;
        }

        private void add(MethodComplexityMetrics method) {
            methodCount++;
            totalComplexity += method.getComplexity();
            maxComplexity = Math.max(maxComplexity, method.getComplexity());
            if ("HIGH".equals(method.getRiskLevel()) || "VERY_HIGH".equals(method.getRiskLevel())) {
                highRiskMethodCount++;
            }
        }

        private ClassComplexitySummary toSummary() {
            ClassComplexitySummary summary = new ClassComplexitySummary();
            summary.setFileName(fileName);
            summary.setClassName(className);
            summary.setMethodCount(methodCount);
            summary.setTotalComplexity(totalComplexity);
            summary.setAvgComplexity(avg(totalComplexity, methodCount));
            summary.setMaxComplexity(maxComplexity);
            summary.setHighRiskMethodCount(highRiskMethodCount);
            return summary;
        }
    }
}
