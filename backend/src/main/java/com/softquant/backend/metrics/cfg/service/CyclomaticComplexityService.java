package com.softquant.backend.metrics.cfg.service;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.softquant.backend.metrics.cfg.dto.DecisionPoint;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class CyclomaticComplexityService {

    public ComplexityScan scan(Node methodNode) {
        List<DecisionPoint> decisionPoints = Stream.of(
                        methodNode.findAll(IfStmt.class).stream()
                                .map(node -> decisionPoint("IF", "if / else if", lineOf(node))),
                        methodNode.findAll(ForStmt.class).stream()
                                .map(node -> decisionPoint("FOR", "for", lineOf(node))),
                        methodNode.findAll(ForEachStmt.class).stream()
                                .map(node -> decisionPoint("FOREACH", "foreach", lineOf(node))),
                        methodNode.findAll(WhileStmt.class).stream()
                                .map(node -> decisionPoint("WHILE", "while", lineOf(node))),
                        methodNode.findAll(DoStmt.class).stream()
                                .map(node -> decisionPoint("DO_WHILE", "do while", lineOf(node))),
                        methodNode.findAll(SwitchEntry.class).stream()
                                .filter(entry -> !entry.getLabels().isEmpty())
                                .map(entry -> decisionPoint("SWITCH_CASE", "case", lineOf(entry))),
                        methodNode.findAll(CatchClause.class).stream()
                                .map(node -> decisionPoint("CATCH", "catch", lineOf(node))),
                        methodNode.findAll(ConditionalExpr.class).stream()
                                .map(node -> decisionPoint("TERNARY", "?:", lineOf(node))),
                        methodNode.findAll(BinaryExpr.class).stream()
                                .filter(this::isLogicalShortCircuit)
                                .map(node -> decisionPoint("LOGICAL", node.getOperator().asString(), lineOf(node)))
                )
                .flatMap(stream -> stream)
                .sorted(Comparator.comparingInt(DecisionPoint::getLine)
                        .thenComparing(DecisionPoint::getKind)
                        .thenComparing(DecisionPoint::getLabel))
                .toList();

        int decisionCount = decisionPoints.stream().mapToInt(DecisionPoint::getIncrement).sum();
        return new ComplexityScan(decisionPoints, decisionCount + 1);
    }

    public String riskLevel(int complexity) {
        if (complexity >= 21) {
            return "VERY_HIGH";
        }
        if (complexity >= 11) {
            return "HIGH";
        }
        if (complexity >= 6) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean isLogicalShortCircuit(BinaryExpr expression) {
        return expression.getOperator() == BinaryExpr.Operator.AND
                || expression.getOperator() == BinaryExpr.Operator.OR;
    }

    private DecisionPoint decisionPoint(String kind, String label, int line) {
        DecisionPoint decisionPoint = new DecisionPoint();
        decisionPoint.setKind(kind);
        decisionPoint.setLabel(label);
        decisionPoint.setLine(line);
        decisionPoint.setIncrement(1);
        return decisionPoint;
    }

    private int lineOf(Node node) {
        return node.getRange().map(range -> range.begin.line).orElse(0);
    }

    public record ComplexityScan(List<DecisionPoint> decisionPoints, int complexity) {
    }
}
