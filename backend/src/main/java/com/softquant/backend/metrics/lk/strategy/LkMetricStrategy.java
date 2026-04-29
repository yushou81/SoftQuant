package com.softquant.backend.metrics.lk.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.AnalysisResponse;
import com.softquant.backend.metrics.common.dto.ClassMetrics;
import com.softquant.backend.metrics.common.dto.JavaSourceInput;
import com.softquant.backend.metrics.common.strategy.MetricStrategy;
import com.softquant.backend.metrics.consistency.ClassDescriptor;
import com.softquant.backend.metrics.consistency.ConsistencyAnalyzer;
import com.softquant.backend.metrics.consistency.ConsistencyResult;
import com.softquant.backend.metrics.consistency.XmiParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LkMetricStrategy implements MetricStrategy {

    private final ConsistencyAnalyzer consistencyAnalyzer;
    private final XmiParser xmiParser;

    public LkMetricStrategy(ConsistencyAnalyzer consistencyAnalyzer, XmiParser xmiParser) {
        this.consistencyAnalyzer = consistencyAnalyzer;
        this.xmiParser = xmiParser;
    }

    @Override
    public String metricSet() {
        return "lk";
    }

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        List<ClassMetrics> classes = new ArrayList<>();

        boolean hasSources = request.getSources() != null && !request.getSources().isEmpty();
        if (hasSources) {
            for (JavaSourceInput source : request.getSources()) {
                CompilationUnit unit = StaticJavaParser.parse(source.getContent());
                unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .filter(node -> !node.isInterface())
                        .forEach(node -> classes.add(toLkMetricsFromAst(node)));
            }
        } else if (request.getClassDiagramText() != null && request.getClassDiagramText().trim().startsWith("<")) {
            Map<String, ClassDescriptor> xmiClasses = xmiParser.parse(request.getClassDiagramText().trim());
            xmiClasses.values().forEach(descriptor -> classes.add(toLkMetricsFromDescriptor(descriptor)));
        }

        classes.sort(Comparator.comparing(ClassMetrics::getClassName));
        return buildResponse(request, classes);
    }

    private ClassMetrics toLkMetricsFromAst(ClassOrInterfaceDeclaration declaration) {
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
                ? null : declaration.getExtendedTypes().get(0).getNameAsString());
        metrics.setCs(cs);
        metrics.setNpa(npa);
        metrics.setNoo(noo);
        metrics.setNoa(noa);
        return metrics;
    }

    private ClassMetrics toLkMetricsFromDescriptor(ClassDescriptor descriptor) {
        int methodCount = descriptor.getMethods().size();
        int fieldCount = descriptor.getFields().size();
        // XMI 无法区分 @Override，NOO 无法计算
        ClassMetrics metrics = new ClassMetrics();
        metrics.setClassName(descriptor.getClassName());
        metrics.setSuperClassName(descriptor.getSuperClass());
        metrics.setCs(methodCount + fieldCount);
        metrics.setNpa(0);
        metrics.setNoo(0);
        metrics.setNoa(methodCount);
        return metrics;
    }

    private AnalysisResponse buildResponse(AnalysisRequest request, List<ClassMetrics> classMetrics) {
        AnalysisResponse response = new AnalysisResponse();
        response.setProjectName(request.getProjectName() == null || request.getProjectName().isBlank()
                ? "unnamed-project" : request.getProjectName());
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
        if (size == 0) return 0;
        return Math.round((float) total / size);
    }
}
