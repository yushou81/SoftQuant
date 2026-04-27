package com.softquant.backend.metrics.ck.util;

import com.softquant.backend.metrics.ck.model.ParsedClass;
import com.softquant.backend.metrics.ck.model.ParsedMethod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JavaSourceParser {

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "\\bclass\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?",
            Pattern.MULTILINE
    );
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^(?:\\s*(?:public|protected|private)\\s+)?(?:static\\s+)?(?:final\\s+)?([\\w<>\\[\\]]+)\\s+(\\w+)\\s*(?:=.*)?;"
    );
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "^(?:\\s*(?:public|protected|private)\\s+)?(?:static\\s+)?(?:final\\s+)?[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{\\s*$"
    );
    private static final Pattern CLASS_REFERENCE_PATTERN = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern EXTERNAL_CALL_PATTERN = Pattern.compile("\\b(\\w+)\\.(\\w+)\\s*\\(");

    public ParsedClass parse(String source) {
        String cleanedSource = removeComments(source);
        Matcher classMatcher = CLASS_PATTERN.matcher(cleanedSource);
        if (!classMatcher.find()) {
            throw new IllegalArgumentException("No class declaration found in source");
        }

        String className = classMatcher.group(1);
        String superClassName = classMatcher.group(2);
        Set<String> fields = new HashSet<>();
        Set<String> directClassReferences = new HashSet<>();
        List<ParsedMethod> methods = new ArrayList<>();

        String[] lines = cleanedSource.split("\\R");
        int braceDepth = 0;
        int methodStartDepth = -1;
        String currentMethodName = null;
        List<String> currentMethodLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                braceDepth += count(line, '{') - count(line, '}');
                continue;
            }

            if (currentMethodName == null && braceDepth <= 1) {
                Matcher fieldMatcher = FIELD_PATTERN.matcher(trimmed);
                if (fieldMatcher.matches() && !trimmed.contains("(")) {
                    fields.add(fieldMatcher.group(2));
                    collectClassReferences(trimmed, directClassReferences);
                }
            }

            if (currentMethodName == null && METHOD_SIGNATURE_PATTERN.matcher(trimmed).matches()) {
                Matcher methodMatcher = METHOD_SIGNATURE_PATTERN.matcher(trimmed);
                if (methodMatcher.matches()) {
                    currentMethodName = methodMatcher.group(1);
                    methodStartDepth = braceDepth;
                    currentMethodLines.clear();
                    currentMethodLines.add(trimmed);
                }
            } else if (currentMethodName != null) {
                currentMethodLines.add(trimmed);
            } else if (braceDepth <= 1) {
                collectClassReferences(trimmed, directClassReferences);
            }

            braceDepth += count(line, '{') - count(line, '}');

            if (currentMethodName != null && braceDepth <= methodStartDepth) {
                methods.add(buildMethod(currentMethodName, currentMethodLines, fields));
                currentMethodName = null;
                methodStartDepth = -1;
                currentMethodLines = new ArrayList<>();
            }
        }

        return new ParsedClass(className, superClassName, fields, directClassReferences, methods);
    }

    private ParsedMethod buildMethod(String methodName, List<String> methodLines, Set<String> knownFields) {
        Set<String> referencedFields = new HashSet<>();
        Set<String> referencedClasses = new HashSet<>();
        Set<String> externalMethodCalls = new HashSet<>();

        String joined = String.join("\n", methodLines);
        for (String field : knownFields) {
            if (Pattern.compile("\\b" + Pattern.quote(field) + "\\b").matcher(joined).find()) {
                referencedFields.add(field);
            }
        }

        collectClassReferences(joined, referencedClasses);
        Matcher callMatcher = EXTERNAL_CALL_PATTERN.matcher(joined);
        while (callMatcher.find()) {
            String owner = callMatcher.group(1);
            if (!"this".equals(owner) && !"super".equals(owner)) {
                externalMethodCalls.add(owner + "." + callMatcher.group(2));
            }
        }

        return new ParsedMethod(methodName, referencedFields, referencedClasses, externalMethodCalls);
    }

    private void collectClassReferences(String text, Set<String> target) {
        Matcher matcher = CLASS_REFERENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            target.add(matcher.group(1));
        }
    }

    private int count(String text, char c) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                total++;
            }
        }
        return total;
    }

    private String removeComments(String source) {
        String withoutBlock = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlock.replaceAll("//.*", "");
    }
}
