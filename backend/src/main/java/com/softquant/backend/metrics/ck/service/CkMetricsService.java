package com.softquant.backend.metrics.ck.service;

import com.softquant.backend.metrics.ck.dto.CkAnalysisRequest;
import com.softquant.backend.metrics.ck.dto.CkAnalysisResponse;
import com.softquant.backend.metrics.ck.dto.CkClassMetrics;
import com.softquant.backend.metrics.ck.model.ParsedClass;
import com.softquant.backend.metrics.ck.model.ParsedMethod;
import com.softquant.backend.metrics.ck.util.JavaSourceParser;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CkMetricsService {

    private final JavaSourceParser parser;

    public CkMetricsService(JavaSourceParser parser) {
        this.parser = parser;
    }

    public CkAnalysisResponse analyze(CkAnalysisRequest request) {
        List<ParsedClass> parsedClasses = request.getSources().stream()
                .map(source -> parser.parse(source.getContent()))
                .toList();

        Map<String, ParsedClass> classMap = new HashMap<>();
        for (ParsedClass parsedClass : parsedClasses) {
            classMap.put(parsedClass.className(), parsedClass);
        }

        Map<String, Integer> childCount = calculateChildCount(parsedClasses);
        List<CkClassMetrics> classMetrics = new ArrayList<>();

        for (ParsedClass parsedClass : parsedClasses) {
            CkClassMetrics metrics = new CkClassMetrics();
            metrics.setClassName(parsedClass.className());
            metrics.setSuperClassName(parsedClass.superClassName());
            metrics.setWmc(parsedClass.methods().size());
            metrics.setDit(calculateDit(parsedClass, classMap));
            metrics.setNoc(childCount.getOrDefault(parsedClass.className(), 0));
            metrics.setCbo(calculateCbo(parsedClass, classMap.keySet()));
            metrics.setRfc(calculateRfc(parsedClass));
            metrics.setLcom(calculateLcom(parsedClass));
            classMetrics.add(metrics);
        }

        classMetrics.sort(Comparator.comparing(CkClassMetrics::getClassName));
        return buildResponse(request, classMetrics);
    }

    private Map<String, Integer> calculateChildCount(List<ParsedClass> parsedClasses) {
        Map<String, Integer> children = new HashMap<>();
        for (ParsedClass parsedClass : parsedClasses) {
            if (parsedClass.superClassName() != null && !parsedClass.superClassName().isBlank()) {
                children.merge(parsedClass.superClassName(), 1, Integer::sum);
            }
        }
        return children;
    }

    private int calculateDit(ParsedClass parsedClass, Map<String, ParsedClass> classMap) {
        int depth = 0;
        Deque<String> chain = new ArrayDeque<>();
        String current = parsedClass.superClassName();
        while (current != null && !current.isBlank() && !chain.contains(current)) {
            depth++;
            chain.push(current);
            ParsedClass parent = classMap.get(current);
            current = parent == null ? null : parent.superClassName();
        }
        return depth;
    }

    private int calculateCbo(ParsedClass parsedClass, Set<String> knownClasses) {
        Set<String> coupling = new HashSet<>();
        coupling.addAll(parsedClass.directClassReferences());
        for (ParsedMethod method : parsedClass.methods()) {
            coupling.addAll(method.referencedClasses());
        }
        coupling.remove(parsedClass.className());
        coupling.remove("String");
        coupling.remove("Integer");
        coupling.remove("Long");
        coupling.remove("Double");
        coupling.remove("Boolean");
        coupling.retainAll(knownClasses);
        return coupling.size();
    }

    private int calculateRfc(ParsedClass parsedClass) {
        Set<String> externalCalls = new HashSet<>();
        for (ParsedMethod method : parsedClass.methods()) {
            externalCalls.addAll(method.externalMethodCalls());
        }
        return parsedClass.methods().size() + externalCalls.size();
    }

    private int calculateLcom(ParsedClass parsedClass) {
        List<ParsedMethod> methods = parsedClass.methods();
        int p = 0;
        int q = 0;
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                Set<String> shared = new HashSet<>(methods.get(i).referencedFields());
                shared.retainAll(methods.get(j).referencedFields());
                if (shared.isEmpty()) {
                    p++;
                } else {
                    q++;
                }
            }
        }
        return Math.max(p - q, 0);
    }

    private CkAnalysisResponse buildResponse(CkAnalysisRequest request, List<CkClassMetrics> classMetrics) {
        CkAnalysisResponse response = new CkAnalysisResponse();
        response.setProjectName(request.getProjectName() == null || request.getProjectName().isBlank()
                ? "unnamed-project"
                : request.getProjectName());
        response.setClassCount(classMetrics.size());
        response.setAvgWmc(avg(classMetrics.stream().mapToInt(CkClassMetrics::getWmc).sum(), classMetrics.size()));
        response.setAvgDit(avg(classMetrics.stream().mapToInt(CkClassMetrics::getDit).sum(), classMetrics.size()));
        response.setAvgNoc(avg(classMetrics.stream().mapToInt(CkClassMetrics::getNoc).sum(), classMetrics.size()));
        response.setAvgCbo(avg(classMetrics.stream().mapToInt(CkClassMetrics::getCbo).sum(), classMetrics.size()));
        response.setAvgRfc(avg(classMetrics.stream().mapToInt(CkClassMetrics::getRfc).sum(), classMetrics.size()));
        response.setAvgLcom(avg(classMetrics.stream().mapToInt(CkClassMetrics::getLcom).sum(), classMetrics.size()));
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
