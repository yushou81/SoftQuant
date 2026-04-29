package com.softquant.backend.metrics.consistency;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.softquant.backend.metrics.common.dto.AnalysisRequest;
import com.softquant.backend.metrics.common.dto.JavaSourceInput;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class ConsistencyAnalyzer {

    private static final Pattern CLASS_DECLARATION = Pattern.compile("^\\s*(?:class|interface)\\s+(\\w+)");
    private static final Pattern INHERITANCE_REL = Pattern.compile("(\\w+)\\s*<\\|--\\s*(\\w+)");
    private static final Pattern METHOD_LINE = Pattern.compile("[+#~-]?\\s*(\\w+)\\s*\\(");
    private static final Pattern FIELD_LINE = Pattern.compile("[+#~-]?\\s*(\\w+)\\s*(?::|$)");

    private final XmiParser xmiParser;

    public ConsistencyAnalyzer(XmiParser xmiParser) {
        this.xmiParser = xmiParser;
    }

    public ConsistencyResult analyze(AnalysisRequest request) {
        Map<String, ClassDescriptor> impl = parseImplementation(request.getSources());
        Map<String, ClassDescriptor> design = parseDesign(request.getClassDiagramText());
        return compare(design, impl);
    }

    private Map<String, ClassDescriptor> parseImplementation(List<JavaSourceInput> sources) {
        Map<String, ClassDescriptor> map = new HashMap<>();
        if (sources == null || sources.isEmpty()) {
            return map;
        }
        for (JavaSourceInput source : sources) {
            CompilationUnit unit = StaticJavaParser.parse(source.getContent());
            for (ClassOrInterfaceDeclaration cls : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                ClassDescriptor descriptor = new ClassDescriptor();
                descriptor.setClassName(cls.getNameAsString());
                descriptor.setFromDesign(false);
                if (!cls.getExtendedTypes().isEmpty()) {
                    descriptor.setSuperClass(cls.getExtendedTypes().get(0).getNameAsString());
                }
                Set<String> methods = new HashSet<>();
                for (MethodDeclaration method : cls.getMethods()) {
                    methods.add(method.getNameAsString());
                }
                descriptor.setMethods(methods);
                Set<String> fields = new HashSet<>();
                cls.getFields().forEach(field -> field.getVariables()
                        .forEach(variable -> fields.add(variable.getNameAsString())));
                descriptor.setFields(fields);
                map.put(descriptor.getClassName(), descriptor);
            }
        }
        return map;
    }

    private Map<String, ClassDescriptor> parseDesign(String classDiagramText) {
        Map<String, ClassDescriptor> map = new HashMap<>();
        if (classDiagramText == null || classDiagramText.isBlank()) {
            return map;
        }
        String trimmed = classDiagramText.trim();
        if (trimmed.startsWith("<")) {
            return xmiParser.parse(trimmed);
        }
        return parsePlantUml(trimmed);
    }

    private Map<String, ClassDescriptor> parsePlantUml(String text) {
        Map<String, ClassDescriptor> map = new HashMap<>();
        String[] lines = text.split("\\R");
        ClassDescriptor current = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("@start") || line.startsWith("@end")) {
                continue;
            }
            Matcher classMatcher = CLASS_DECLARATION.matcher(line);
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                current = map.computeIfAbsent(className, key -> {
                    ClassDescriptor descriptor = new ClassDescriptor();
                    descriptor.setClassName(key);
                    descriptor.setFromDesign(true);
                    return descriptor;
                });
                continue;
            }
            Matcher inheritanceMatcher = INHERITANCE_REL.matcher(line);
            if (inheritanceMatcher.find()) {
                String parent = inheritanceMatcher.group(1);
                String child = inheritanceMatcher.group(2);
                ClassDescriptor childDescriptor = map.computeIfAbsent(child, key -> {
                    ClassDescriptor descriptor = new ClassDescriptor();
                    descriptor.setClassName(key);
                    descriptor.setFromDesign(true);
                    return descriptor;
                });
                childDescriptor.setSuperClass(parent);
                map.computeIfAbsent(parent, key -> {
                    ClassDescriptor descriptor = new ClassDescriptor();
                    descriptor.setClassName(key);
                    descriptor.setFromDesign(true);
                    return descriptor;
                });
                continue;
            }
            if (current != null && line.equals("}")) {
                current = null;
                continue;
            }
            if (current != null) {
                Matcher methodMatcher = METHOD_LINE.matcher(line);
                if (methodMatcher.find()) {
                    current.getMethods().add(methodMatcher.group(1));
                    continue;
                }
                Matcher fieldMatcher = FIELD_LINE.matcher(line);
                if (fieldMatcher.find()) {
                    current.getFields().add(fieldMatcher.group(1));
                }
            }
        }
        return map;
    }

    private ConsistencyResult compare(Map<String, ClassDescriptor> design, Map<String, ClassDescriptor> impl) {
        ConsistencyResult result = new ConsistencyResult();
        List<String> issues = new ArrayList<>();
        if (design.isEmpty()) {
            result.setClassCoverage(100);
            result.setMethodDriftRate(0);
            result.setInheritanceConsistency(100);
            result.setIssues(List.of("未提供可解析的类图，已跳过一致性比对。"));
            return result;
        }
        if (impl.isEmpty()) {
            result.setClassCoverage(0);
            result.setMethodDriftRate(0);
            result.setInheritanceConsistency(0);
            result.setIssues(List.of("未提供 Java 源码，已跳过一致性比对。"));
            return result;
        }

        int implemented = 0;
        int totalDesignMethods = 0;
        int methodDelta = 0;
        int inheritanceCount = 0;
        int inheritanceMatched = 0;

        for (ClassDescriptor designClass : design.values()) {
            ClassDescriptor implClass = impl.get(designClass.getClassName());
            if (implClass != null) {
                implemented++;
                Set<String> missingMethods = new HashSet<>(designClass.getMethods());
                missingMethods.removeAll(implClass.getMethods());
                Set<String> extraMethods = new HashSet<>(implClass.getMethods());
                extraMethods.removeAll(designClass.getMethods());
                methodDelta += missingMethods.size() + extraMethods.size();
                totalDesignMethods += designClass.getMethods().size();
                if (!missingMethods.isEmpty()) {
                    issues.add("类 " + designClass.getClassName() + " 缺失设计方法: " + String.join(", ", missingMethods));
                }
                if (!extraMethods.isEmpty()) {
                    issues.add("类 " + designClass.getClassName() + " 存在新增方法: " + String.join(", ", extraMethods));
                }
                if (designClass.getSuperClass() != null && !designClass.getSuperClass().isBlank()) {
                    inheritanceCount++;
                    if (designClass.getSuperClass().equals(implClass.getSuperClass())) {
                        inheritanceMatched++;
                    } else {
                        issues.add("类 " + designClass.getClassName() + " 的继承关系不一致(设计:"
                                + designClass.getSuperClass() + ", 实现:"
                                + (implClass.getSuperClass() == null ? "无" : implClass.getSuperClass()) + ")");
                    }
                }
            } else {
                issues.add("类图类未实现: " + designClass.getClassName());
                totalDesignMethods += designClass.getMethods().size();
                methodDelta += designClass.getMethods().size();
                if (designClass.getSuperClass() != null && !designClass.getSuperClass().isBlank()) {
                    inheritanceCount++;
                }
            }
        }

        for (ClassDescriptor implClass : impl.values()) {
            if (!design.containsKey(implClass.getClassName())) {
                issues.add("实现中存在未建模类: " + implClass.getClassName());
                methodDelta += implClass.getMethods().size();
            }
        }

        result.setClassCoverage(percent(implemented, design.size()));
        result.setMethodDriftRate(totalDesignMethods == 0 ? 0 : percent(methodDelta, totalDesignMethods));
        result.setInheritanceConsistency(inheritanceCount == 0 ? 100 : percent(inheritanceMatched, inheritanceCount));
        result.setIssues(issues.stream().limit(8).toList());
        return result;
    }

    private int percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((float) numerator * 100 / denominator);
    }
}
