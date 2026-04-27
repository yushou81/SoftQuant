package com.softquant.backend.metrics.lk.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.AnalysisResponse;
import com.softquant.backend.metrics.common.dto.ClassMetrics;
import com.softquant.backend.metrics.common.dto.JavaSourceInput;
import com.softquant.backend.metrics.common.strategy.MetricStrategy;
import com.softquant.backend.metrics.consistency.ConsistencyAnalyzer;
import com.softquant.backend.metrics.consistency.ConsistencyResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LkMetricStrategy implements MetricStrategy {

    private final ConsistencyAnalyzer consistencyAnalyzer;

    public LkMetricStrategy(ConsistencyAnalyzer consistencyAnalyzer) {
        this.consistencyAnalyzer = consistencyAnalyzer;
    }

    @Override
    public String metricSet() {
        return "lk";
    }

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        List<ClassMetrics> classes = new ArrayList<>();
        for (JavaSourceInput source : request.getSources()) {
            CompilationUnit unit = StaticJavaParser.parse(source.getContent());
            unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(node -> !node.isInterface())
                    .forEach(node -> classes.add(toLkMetrics(node)));
        }
        classes.sort(Comparator.comparing(ClassMetrics::getClassName));
        return buildResponse(request, classes);
    }

    private ClassMetrics toLkMetrics(ClassOrInterfaceDeclaration declaration) {
        int methodCount = declaration.getMethods().size();
        int fieldCount = declaration.getFields().stream()
                .mapToInt(field -> field.getVariables().size())
                .sum();

        int cs = methodCount + fieldCount;
        int npa = declaration.getFields().stream()
                .filter(field -> field.hasModifier(com.github.javaparser.ast.Modifier.Keyword.PUBLIC))
                .mapToInt(field -> field.getVariables().size())
                .sum();
        int noo = (int) declaration.getMethods().stream()
                .filter(method -> method.isAnnotationPresent("Override"))
                .count();
        int noa = Math.max(methodCount - noo, 0);

        ClassMetrics metrics = new ClassMetrics();
        metrics.setClassName(declaration.getNameAsString());
        metrics.setSuperClassName(declaration.getExtendedTypes().isEmpty()
                ? null
                : declaration.getExtendedTypes().get(0).getNameAsString());
        metrics.setCs(cs);
        metrics.setNpa(npa);
        metrics.setNoo(noo);
        metrics.setNoa(noa);
        return metrics;
    }

    private AnalysisResponse buildResponse(AnalysisRequest request, List<ClassMetrics> classMetrics) {
        AnalysisResponse response = new AnalysisResponse();
        response.setProjectName(request.getProjectName() == null || request.getProjectName().isBlank()
                ? "unnamed-project"
                : request.getProjectName());
        response.setClassCount(classMetrics.size());
        response.setAvgCs(avg(classMetrics.stream().mapToInt(ClassMetrics::getCs).sum(), classMetrics.size()));
        response.setAvgNpa(avg(classMetrics.stream().mapToInt(ClassMetrics::getNpa).sum(), classMetrics.size()));
        response.setAvgNoo(avg(classMetrics.stream().mapToInt(ClassMetrics::getNoo).sum(), classMetrics.size()));
        response.setAvgNoa(avg(classMetrics.stream().mapToInt(ClassMetrics::getNoa).sum(), classMetrics.size()));
        ConsistencyResult consistency = consistencyAnalyzer.analyze(request);
        response.setClassCoverage(consistency.getClassCoverage());
        response.setMethodDriftRate(consistency.getMethodDriftRate());
        response.setInheritanceConsistency(consistency.getInheritanceConsistency());
        response.setConsistencyIssues(consistency.getIssues());
        response.setClasses(classMetrics);
        return response;
    }

    private int avg(int total, int size) {
        if (size == 0) {
            return 0;
        }
        return Math.round((float) total / size);
    }
}
