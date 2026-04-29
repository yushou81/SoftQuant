package com.softquant.backend.metrics.ck.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CkMetricStrategy implements MetricStrategy {

    private final ConsistencyAnalyzer consistencyAnalyzer;
    private final XmiParser xmiParser;

    public CkMetricStrategy(ConsistencyAnalyzer consistencyAnalyzer, XmiParser xmiParser) {
        this.consistencyAnalyzer = consistencyAnalyzer;
        this.xmiParser = xmiParser;
    }

    @Override
    public String metricSet() {
        return "ck";
    }

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        Map<String, ClassSnapshot> fullMap = new HashMap<>();
        Map<String, ClassSnapshot> publicMap = new HashMap<>();

        boolean hasSources = request.getSources() != null && !request.getSources().isEmpty();
        if (hasSources) {
            for (JavaSourceInput source : request.getSources()) {
                CompilationUnit unit = StaticJavaParser.parse(source.getContent());
                unit.findAll(ClassOrInterfaceDeclaration.class).forEach(node -> {
                    ClassSnapshot snapshot = collectClassSnapshot(node);
                    fullMap.put(snapshot.className, snapshot);
                    publicMap.put(snapshot.className, snapshot);
                });
            }
        } else if (request.getClassDiagramText() != null && request.getClassDiagramText().trim().startsWith("<")) {
            Map<String, ClassDescriptor> xmiClasses = xmiParser.parse(request.getClassDiagramText().trim());
            xmiClasses.values().forEach(descriptor -> {
                ClassSnapshot snapshot = snapshotFromDescriptor(descriptor);
                fullMap.put(snapshot.className, snapshot);
                publicMap.put(snapshot.className, snapshot);
            });
        }

        Map<String, Integer> childCount = calculateChildCount(fullMap);
        List<ClassMetrics> classes = new ArrayList<>();

        for (ClassSnapshot snapshot : publicMap.values()) {
            ClassMetrics metrics = new ClassMetrics();
            metrics.setClassName(snapshot.className);
            metrics.setSuperClassName(snapshot.superClassName);
            metrics.setWmc(snapshot.methods.size());
            metrics.setDit(calculateDit(snapshot, fullMap));
            metrics.setNoc(childCount.getOrDefault(snapshot.className, 0));
            metrics.setCbo(calculateCbo(snapshot, publicMap.keySet()));
            metrics.setRfc(snapshot.methods.size() + snapshot.responseCalls.size());
            metrics.setLcom(calculateLcom(snapshot));
            classes.add(metrics);
        }
        classes.sort(Comparator.comparing(ClassMetrics::getClassName));
        return buildResponse(request, classes);
    }

    private ClassSnapshot collectClassSnapshot(ClassOrInterfaceDeclaration declaration) {
        ClassSnapshot snapshot = new ClassSnapshot();
        snapshot.className = declaration.getNameAsString();
        snapshot.isInterface = declaration.isInterface();
        snapshot.superClassName = declaration.getExtendedTypes().isEmpty()
                ? null
                : declaration.getExtendedTypes().get(0).getNameAsString();

        declaration.getFields().forEach(field -> collectFieldInfo(field, snapshot));
        declaration.getMethods().forEach(method -> collectMethodInfo(method, snapshot));
        return snapshot;
    }

    private ClassSnapshot snapshotFromDescriptor(ClassDescriptor descriptor) {
        ClassSnapshot snapshot = new ClassSnapshot();
        snapshot.className = descriptor.getClassName();
        snapshot.superClassName = descriptor.getSuperClass();
        snapshot.fieldNames.addAll(descriptor.getFields());
        for (String methodName : descriptor.getMethods()) {
            MethodSnapshot ms = new MethodSnapshot();
            ms.name = methodName;
            snapshot.methods.add(ms);
        }
        return snapshot;
    }

    private void collectFieldInfo(FieldDeclaration field, ClassSnapshot snapshot) {
        String typeName = resolveSimpleTypeName(field.getElementType().asString());
        snapshot.referencedTypes.add(typeName);
        for (VariableDeclarator variable : field.getVariables()) {
            snapshot.fieldNames.add(variable.getNameAsString());
        }
    }

    private void collectMethodInfo(MethodDeclaration method, ClassSnapshot snapshot) {
        MethodSnapshot methodSnapshot = new MethodSnapshot();
        methodSnapshot.name = method.getNameAsString();

        method.getParameters().forEach(parameter ->
                snapshot.referencedTypes.add(resolveSimpleTypeName(parameter.getTypeAsString())));

        method.getBody().ifPresent(body -> {
            body.findAll(VariableDeclarator.class).forEach(variable ->
                    snapshot.referencedTypes.add(resolveSimpleTypeName(variable.getTypeAsString())));
            body.findAll(ObjectCreationExpr.class).forEach(expr ->
                    snapshot.referencedTypes.add(resolveSimpleTypeName(expr.getTypeAsString())));
        });

        method.accept(new MethodUsageVisitor(snapshot.fieldNames), methodSnapshot);
        snapshot.referencedTypes.addAll(methodSnapshot.referencedTypes);
        snapshot.responseCalls.addAll(methodSnapshot.responseCalls);
        snapshot.methods.add(methodSnapshot);
    }

    private Map<String, Integer> calculateChildCount(Map<String, ClassSnapshot> classMap) {
        Map<String, Integer> childCount = new HashMap<>();
        for (ClassSnapshot snapshot : classMap.values()) {
            if (snapshot.superClassName != null && !snapshot.superClassName.isBlank()) {
                childCount.merge(snapshot.superClassName, 1, Integer::sum);
            }
        }
        return childCount;
    }

    private int calculateDit(ClassSnapshot snapshot, Map<String, ClassSnapshot> classMap) {
        if (snapshot.isInterface) {
            return 0;
        }
        int depth = 0;
        Set<String> visited = new HashSet<>();
        String current = snapshot.superClassName;
        while (current != null && !current.isBlank() && !visited.contains(current)) {
            visited.add(current);
            depth++;
            ClassSnapshot parent = classMap.get(current);
            current = parent == null ? null : parent.superClassName;
        }
        return depth;
    }

    private static final Set<String> EXCLUDED_TYPES = Set.of(
            "String", "Integer", "Long", "Double", "Float", "Short", "Byte",
            "Character", "Boolean", "Object", "void", "int", "long", "double",
            "float", "short", "byte", "char", "boolean", "var");

    private int calculateCbo(ClassSnapshot snapshot, Set<String> knownClasses) {
        Set<String> coupled = new HashSet<>(snapshot.referencedTypes);
        coupled.remove(snapshot.className);
        coupled.removeAll(EXCLUDED_TYPES);
        return coupled.size();
    }

    private int calculateLcom(ClassSnapshot snapshot) {
        if (snapshot.methods.size() <= 1) {
            return 0;
        }
        int p = 0;
        int q = 0;
        List<MethodSnapshot> methods = snapshot.methods;
        for (int i = 0; i < methods.size(); i++) {
            for (int j = i + 1; j < methods.size(); j++) {
                Set<String> shared = new HashSet<>(methods.get(i).referencedFields);
                shared.retainAll(methods.get(j).referencedFields);
                if (shared.isEmpty()) {
                    p++;
                } else {
                    q++;
                }
            }
        }
        return Math.max(p - q, 0);
    }

    private AnalysisResponse buildResponse(AnalysisRequest request, List<ClassMetrics> classMetrics) {
        AnalysisResponse response = new AnalysisResponse();
        response.setProjectName(request.getProjectName() == null || request.getProjectName().isBlank()
                ? "unnamed-project"
                : request.getProjectName());
        response.setClassCount(classMetrics.size());
        response.setAvgWmc(avg(classMetrics.stream().mapToInt(ClassMetrics::getWmc).sum(), classMetrics.size()));
        response.setAvgDit(avg(classMetrics.stream().mapToInt(ClassMetrics::getDit).sum(), classMetrics.size()));
        response.setAvgNoc(avg(classMetrics.stream().mapToInt(ClassMetrics::getNoc).sum(), classMetrics.size()));
        response.setAvgCbo(avg(classMetrics.stream().mapToInt(ClassMetrics::getCbo).sum(), classMetrics.size()));
        response.setAvgRfc(avg(classMetrics.stream().mapToInt(ClassMetrics::getRfc).sum(), classMetrics.size()));
        response.setAvgLcom(avg(classMetrics.stream().mapToInt(ClassMetrics::getLcom).sum(), classMetrics.size()));
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

    private String resolveSimpleTypeName(String fullType) {
        String normalized = fullType.replace("?", "").trim();
        if (normalized.contains("<")) {
            normalized = normalized.substring(0, normalized.indexOf('<'));
        }
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
        }
        return normalized;
    }

    private static final class ClassSnapshot {
        private String className;
        private String superClassName;
        private boolean isInterface;
        private final Set<String> fieldNames = new HashSet<>();
        private final Set<String> referencedTypes = new HashSet<>();
        private final Set<String> responseCalls = new HashSet<>();
        private final List<MethodSnapshot> methods = new ArrayList<>();
    }

    private static final class MethodSnapshot {
        private String name;
        private final Set<String> referencedFields = new HashSet<>();
        private final Set<String> referencedTypes = new HashSet<>();
        private final Set<String> responseCalls = new HashSet<>();
    }

    private static final class MethodUsageVisitor extends VoidVisitorAdapter<MethodSnapshot> {

        private final Set<String> classFields;

        private MethodUsageVisitor(Set<String> classFields) {
            this.classFields = classFields;
        }

        @Override
        public void visit(NameExpr nameExpr, MethodSnapshot collector) {
            super.visit(nameExpr, collector);
            String symbol = nameExpr.getNameAsString();
            if (classFields.contains(symbol)) {
                collector.referencedFields.add(symbol);
            }
        }

        @Override
        public void visit(FieldAccessExpr fieldAccessExpr, MethodSnapshot collector) {
            super.visit(fieldAccessExpr, collector);
            if (fieldAccessExpr.getScope().isThisExpr()) {
                String field = fieldAccessExpr.getNameAsString();
                if (classFields.contains(field)) {
                    collector.referencedFields.add(field);
                }
            }
        }

        @Override
        public void visit(MethodCallExpr methodCallExpr, MethodSnapshot collector) {
            super.visit(methodCallExpr, collector);
            Optional<com.github.javaparser.ast.expr.Expression> scope = methodCallExpr.getScope();
            if (scope.isEmpty()) {
                collector.responseCalls.add("this." + methodCallExpr.getNameAsString());
                return;
            }

            String owner = scope.get().toString();
            if ("super".equals(owner) || scope.get() instanceof ThisExpr) {
                collector.responseCalls.add("this." + methodCallExpr.getNameAsString());
                return;
            }
            if (owner.startsWith("System.") || owner.startsWith("java.") || owner.equals("System")) {
                return;
            }
            collector.responseCalls.add(owner + "." + methodCallExpr.getNameAsString());
        }

        @Override
        public void visit(ClassOrInterfaceType type, MethodSnapshot collector) {
            super.visit(type, collector);
            collector.referencedTypes.add(type.getNameAsString());
        }
    }
}
