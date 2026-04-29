package com.softquant.backend.metrics.loc.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.softquant.backend.metrics.loc.dto.LocAnalyzeRequest;
import com.softquant.backend.metrics.loc.dto.LocAnalyzeResponse;
import com.softquant.backend.metrics.loc.dto.LocFileMetrics;
import com.softquant.backend.metrics.loc.dto.LocLanguageSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LocAnalyzeService {

    public LocAnalyzeResponse analyze(LocAnalyzeRequest request) {
        List<LocFileMetrics> files = request.sources().stream()
                .map(this::analyzeSource)
                .sorted(Comparator.comparing(LocFileMetrics::getFileName))
                .toList();

        LocAnalyzeResponse response = new LocAnalyzeResponse();
        response.setModule("LOC");
        response.setContractKind("ANALYSIS");
        response.setProjectName(normalizeProjectName(request.projectName()));
        response.setFileCount(files.size());
        response.setPhysicalLines(files.stream().mapToInt(LocFileMetrics::getPhysicalLines).sum());
        response.setLogicalLines(files.stream().mapToInt(LocFileMetrics::getLogicalLines).sum());
        response.setCodeLines(files.stream().mapToInt(LocFileMetrics::getCodeLines).sum());
        response.setCommentLines(files.stream().mapToInt(LocFileMetrics::getCommentLines).sum());
        response.setBlankLines(files.stream().mapToInt(LocFileMetrics::getBlankLines).sum());
        response.setMixedLines(files.stream().mapToInt(LocFileMetrics::getMixedLines).sum());
        response.setCommentRate(commentRate(
                response.getCommentLines(),
                response.getMixedLines(),
                response.getCodeLines()
        ));
        response.setFiles(files);
        response.setLanguageSummaries(buildLanguageSummaries(files));
        response.setFormulaTrace(List.of(
                new LocAnalyzeResponse.TraceLine(
                        "Physical LOC",
                        "physicalLines = codeLines + commentLines + blankLines",
                        String.valueOf(response.getPhysicalLines())
                ),
                new LocAnalyzeResponse.TraceLine(
                        "Comment Rate",
                        "commentRate = (commentLines + mixedLines) / (codeLines + commentLines) * 100%",
                        response.getCommentRate().toPlainString() + "%"
                ),
                new LocAnalyzeResponse.TraceLine(
                        "Logical LOC",
                        "Java uses AST declaration/statement counting; other languages fall back to codeLines",
                        String.valueOf(response.getLogicalLines())
                )
        ));
        return response;
    }

    private LocFileMetrics analyzeSource(LocAnalyzeRequest.SourceInput source) {
        String language = resolveLanguage(source.fileName(), source.language());
        RawLineMetrics rawMetrics = scan(source.content(), language);
        LogicalLineResult logicalResult = calculateLogicalLines(source.content(), language, rawMetrics.codeLines());

        LocFileMetrics metrics = new LocFileMetrics();
        metrics.setFileName(source.fileName());
        metrics.setLanguage(language);
        metrics.setPhysicalLines(rawMetrics.physicalLines());
        metrics.setLogicalLines(logicalResult.logicalLines());
        metrics.setCodeLines(rawMetrics.codeLines());
        metrics.setCommentLines(rawMetrics.commentLines());
        metrics.setBlankLines(rawMetrics.blankLines());
        metrics.setMixedLines(rawMetrics.mixedLines());
        metrics.setCommentRate(commentRate(rawMetrics.commentLines(), rawMetrics.mixedLines(), rawMetrics.codeLines()));
        metrics.setParseStatus(logicalResult.parseStatus());
        return metrics;
    }

    private RawLineMetrics scan(String content, String language) {
        if ("PYTHON".equals(language)) {
            return scanPython(content);
        }
        if (isCStyleLanguage(language)) {
            return scanCStyle(content);
        }
        return scanGeneric(content);
    }

    private RawLineMetrics scanCStyle(String content) {
        List<String> lines = toPhysicalLines(content);
        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        int mixedLines = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            boolean hasCode = false;
            boolean hasComment = false;
            boolean inString = false;
            boolean inChar = false;
            boolean escaped = false;

            if (inBlockComment) {
                hasComment = true;
            }

            for (int i = 0; i < line.length(); i++) {
                char current = line.charAt(i);
                char next = i + 1 < line.length() ? line.charAt(i + 1) : '\0';

                if (inBlockComment) {
                    hasComment = true;
                    if (current == '*' && next == '/') {
                        inBlockComment = false;
                        i++;
                    }
                    continue;
                }

                if (inString) {
                    hasCode = true;
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        inString = false;
                    }
                    continue;
                }

                if (inChar) {
                    hasCode = true;
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '\'') {
                        inChar = false;
                    }
                    continue;
                }

                if (current == '/' && next == '/') {
                    hasComment = true;
                    break;
                }
                if (current == '/' && next == '*') {
                    hasComment = true;
                    inBlockComment = true;
                    i++;
                    continue;
                }
                if (current == '"') {
                    hasCode = true;
                    inString = true;
                    continue;
                }
                if (current == '\'') {
                    hasCode = true;
                    inChar = true;
                    continue;
                }
                if (!Character.isWhitespace(current)) {
                    hasCode = true;
                }
            }

            if (hasCode) {
                codeLines++;
            }
            if (hasCode && hasComment) {
                mixedLines++;
            } else if (hasComment) {
                commentLines++;
            } else if (!hasCode) {
                blankLines++;
            }
        }

        return new RawLineMetrics(lines.size(), codeLines, commentLines, blankLines, mixedLines);
    }

    private RawLineMetrics scanPython(String content) {
        List<String> lines = toPhysicalLines(content);
        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        int mixedLines = 0;
        boolean inTripleString = false;
        boolean tripleCountsAsComment = false;
        String tripleDelimiter = "";

        for (String line : lines) {
            boolean hasCode = false;
            boolean hasComment = false;
            boolean inString = false;
            boolean escaped = false;
            char stringQuote = '\0';

            for (int i = 0; i < line.length(); i++) {
                if (inTripleString) {
                    if (tripleCountsAsComment) {
                        hasComment = true;
                    } else {
                        hasCode = true;
                    }
                    int end = line.indexOf(tripleDelimiter, i);
                    if (end < 0) {
                        break;
                    }
                    inTripleString = false;
                    i = end + tripleDelimiter.length() - 1;
                    continue;
                }

                char current = line.charAt(i);
                if (inString) {
                    hasCode = true;
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == stringQuote) {
                        inString = false;
                    }
                    continue;
                }

                String triple = tripleDelimiterAt(line, i);
                if (triple != null) {
                    if (hasCode) {
                        hasCode = true;
                        int end = line.indexOf(triple, i + triple.length());
                        if (end < 0) {
                            inTripleString = true;
                            tripleCountsAsComment = false;
                            tripleDelimiter = triple;
                            break;
                        }
                        i = end + triple.length() - 1;
                        continue;
                    }

                    hasComment = true;
                    int end = line.indexOf(triple, i + triple.length());
                    if (end < 0) {
                        inTripleString = true;
                        tripleCountsAsComment = true;
                        tripleDelimiter = triple;
                        break;
                    }
                    i = end + triple.length() - 1;
                    continue;
                }

                if (current == '#') {
                    hasComment = true;
                    break;
                }
                if (current == '\'' || current == '"') {
                    hasCode = true;
                    inString = true;
                    stringQuote = current;
                    continue;
                }
                if (!Character.isWhitespace(current)) {
                    hasCode = true;
                }
            }

            if (hasCode) {
                codeLines++;
            }
            if (hasCode && hasComment) {
                mixedLines++;
            } else if (hasComment) {
                commentLines++;
            } else if (!hasCode) {
                blankLines++;
            }
        }

        return new RawLineMetrics(lines.size(), codeLines, commentLines, blankLines, mixedLines);
    }

    private RawLineMetrics scanGeneric(String content) {
        List<String> lines = toPhysicalLines(content);
        int codeLines = 0;
        int blankLines = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                blankLines++;
            } else {
                codeLines++;
            }
        }
        return new RawLineMetrics(lines.size(), codeLines, 0, blankLines, 0);
    }

    private LogicalLineResult calculateLogicalLines(String content, String language, int codeLines) {
        if (!"JAVA".equals(language)) {
            return new LogicalLineResult(codeLines, "TEXT_FALLBACK");
        }

        try {
            var unit = StaticJavaParser.parse(content);
            int typeDeclarations = unit.findAll(TypeDeclaration.class).size();
            int fields = unit.findAll(FieldDeclaration.class).stream()
                    .mapToInt(field -> field.getVariables().size())
                    .sum();
            int methods = unit.findAll(MethodDeclaration.class).size();
            int constructors = unit.findAll(ConstructorDeclaration.class).size();
            int statements = (int) unit.findAll(Statement.class).stream()
                    .filter(statement -> !(statement instanceof BlockStmt))
                    .filter(statement -> !(statement instanceof EmptyStmt))
                    .count();
            return new LogicalLineResult(typeDeclarations + fields + methods + constructors + statements, "AST_PARSED");
        } catch (RuntimeException ex) {
            return new LogicalLineResult(codeLines, "TEXT_FALLBACK");
        }
    }

    private List<LocLanguageSummary> buildLanguageSummaries(List<LocFileMetrics> files) {
        Map<String, LanguageAccumulator> grouped = new LinkedHashMap<>();
        for (LocFileMetrics file : files) {
            grouped.computeIfAbsent(file.getLanguage(), LanguageAccumulator::new).add(file);
        }

        return grouped.values().stream()
                .map(LanguageAccumulator::toSummary)
                .toList();
    }

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return "unnamed-project";
        }
        return projectName.trim();
    }

    private String resolveLanguage(String fileName, String language) {
        if (language != null && !language.isBlank()) {
            return normalizeLanguage(language);
        }

        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (normalizedName.endsWith(".java")) {
            return "JAVA";
        }
        if (normalizedName.endsWith(".py")) {
            return "PYTHON";
        }
        if (normalizedName.endsWith(".cpp")
                || normalizedName.endsWith(".cc")
                || normalizedName.endsWith(".cxx")
                || normalizedName.endsWith(".hpp")
                || normalizedName.endsWith(".hh")
                || normalizedName.endsWith(".hxx")) {
            return "CPP";
        }
        if (normalizedName.endsWith(".c") || normalizedName.endsWith(".h")) {
            return "C";
        }
        if (normalizedName.endsWith(".cs")) {
            return "CSHARP";
        }
        if (normalizedName.endsWith(".js") || normalizedName.endsWith(".jsx")) {
            return "JAVASCRIPT";
        }
        if (normalizedName.endsWith(".ts") || normalizedName.endsWith(".tsx")) {
            return "TYPESCRIPT";
        }
        return "GENERIC";
    }

    private String normalizeLanguage(String language) {
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "JAVA" -> "JAVA";
            case "PY", "PYTHON" -> "PYTHON";
            case "C++", "CPP", "CXX" -> "CPP";
            case "C" -> "C";
            case "C#", "CS", "CSHARP" -> "CSHARP";
            case "JS", "JAVASCRIPT" -> "JAVASCRIPT";
            case "TS", "TYPESCRIPT" -> "TYPESCRIPT";
            default -> normalized;
        };
    }

    private boolean isCStyleLanguage(String language) {
        return switch (language) {
            case "JAVA", "CPP", "C", "CSHARP", "JAVASCRIPT", "TYPESCRIPT" -> true;
            default -> false;
        };
    }

    private List<String> toPhysicalLines(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] split = normalized.split("\n", -1);
        int length = split.length;
        if (normalized.endsWith("\n")) {
            length--;
        }

        List<String> lines = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            lines.add(split[i]);
        }
        return lines;
    }

    private String tripleDelimiterAt(String line, int index) {
        if (index + 3 > line.length()) {
            return null;
        }
        if (line.startsWith("\"\"\"", index)) {
            return "\"\"\"";
        }
        if (line.startsWith("'''", index)) {
            return "'''";
        }
        return null;
    }

    private BigDecimal commentRate(int commentLines, int mixedLines, int codeLines) {
        int denominator = codeLines + commentLines;
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf((commentLines + mixedLines) * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record RawLineMetrics(
            int physicalLines,
            int codeLines,
            int commentLines,
            int blankLines,
            int mixedLines
    ) {
    }

    private record LogicalLineResult(int logicalLines, String parseStatus) {
    }

    private final class LanguageAccumulator {
        private final String language;
        private int fileCount;
        private int physicalLines;
        private int logicalLines;
        private int codeLines;
        private int commentLines;
        private int blankLines;
        private int mixedLines;

        private LanguageAccumulator(String language) {
            this.language = language;
        }

        private void add(LocFileMetrics file) {
            fileCount++;
            physicalLines += file.getPhysicalLines();
            logicalLines += file.getLogicalLines();
            codeLines += file.getCodeLines();
            commentLines += file.getCommentLines();
            blankLines += file.getBlankLines();
            mixedLines += file.getMixedLines();
        }

        private LocLanguageSummary toSummary() {
            LocLanguageSummary summary = new LocLanguageSummary();
            summary.setLanguage(language);
            summary.setFileCount(fileCount);
            summary.setPhysicalLines(physicalLines);
            summary.setLogicalLines(logicalLines);
            summary.setCodeLines(codeLines);
            summary.setCommentLines(commentLines);
            summary.setBlankLines(blankLines);
            summary.setMixedLines(mixedLines);
            summary.setCommentRate(commentRate(commentLines, mixedLines, codeLines));
            return summary;
        }
    }
}
