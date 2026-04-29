package com.softquant.backend.metrics.cfg.service;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.softquant.backend.metrics.cfg.dto.ControlFlowGraph;
import com.softquant.backend.metrics.cfg.dto.DecisionPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ControlFlowGraphBuilder {

    public ControlFlowGraph build(MethodDeclaration method) {
        return new AstGraphBuild().build(method.getBody());
    }

    public ControlFlowGraph build(ConstructorDeclaration constructor) {
        return new AstGraphBuild().build(Optional.of(constructor.getBody()));
    }

    public ControlFlowGraph build(List<DecisionPoint> decisionPoints) {
        return new DecisionPointSketchBuild().build(decisionPoints);
    }

    private static final class AstGraphBuild {
        private static final int MAX_LABEL_LENGTH = 46;

        private final List<ControlFlowGraph.GraphNode> nodes = new ArrayList<>();
        private final List<ControlFlowGraph.GraphEdge> edges = new ArrayList<>();
        private final Deque<String> breakTargets = new ArrayDeque<>();
        private final Deque<String> continueTargets = new ArrayDeque<>();
        private int nodeIndex;
        private int edgeIndex = 1;
        private ControlFlowGraph.GraphNode exit;

        private ControlFlowGraph build(Optional<BlockStmt> body) {
            ControlFlowGraph.GraphNode entry = addNode("ENTRY", "Entry", 0);
            exit = createNode("EXIT", "Exit", 0);

            List<PendingEdge> exits = body
                    .map(block -> buildBlock(block, List.of(nextFrom(entry, "start"))))
                    .orElseGet(() -> List.of(nextFrom(entry, "start")));
            connectTo(exits, exit.getId());
            nodes.add(exit);

            ControlFlowGraph graph = new ControlFlowGraph();
            graph.setNodes(nodes);
            graph.setEdges(edges);
            return graph;
        }

        private List<PendingEdge> buildBlock(BlockStmt block, List<PendingEdge> incoming) {
            return buildStatements(block.getStatements(), incoming);
        }

        private List<PendingEdge> buildStatements(List<Statement> statements, List<PendingEdge> incoming) {
            List<PendingEdge> current = incoming;
            for (Statement statement : statements) {
                current = buildStatement(statement, current);
            }
            return current;
        }

        private List<PendingEdge> buildStatement(Statement statement, List<PendingEdge> incoming) {
            if (statement == null || statement instanceof EmptyStmt) {
                return incoming;
            }
            if (statement.isBlockStmt()) {
                return buildBlock(statement.asBlockStmt(), incoming);
            }
            if (statement.isIfStmt()) {
                return buildIf(statement.asIfStmt(), incoming);
            }
            if (statement instanceof WhileStmt whileStmt) {
                return buildWhile(whileStmt, incoming);
            }
            if (statement instanceof ForStmt forStmt) {
                return buildFor(forStmt, incoming);
            }
            if (statement instanceof ForEachStmt forEachStmt) {
                return buildForEach(forEachStmt, incoming);
            }
            if (statement instanceof DoStmt doStmt) {
                return buildDoWhile(doStmt, incoming);
            }
            if (statement instanceof SwitchStmt switchStmt) {
                return buildSwitch(switchStmt, incoming);
            }
            if (statement instanceof TryStmt tryStmt) {
                return buildTry(tryStmt, incoming);
            }
            if (statement instanceof SynchronizedStmt synchronizedStmt) {
                return buildStatement(synchronizedStmt.getBody(), incoming);
            }
            if (statement.isLabeledStmt()) {
                return buildStatement(statement.asLabeledStmt().getStatement(), incoming);
            }
            if (statement instanceof ReturnStmt returnStmt) {
                return buildReturn(returnStmt, incoming);
            }
            if (statement instanceof ThrowStmt throwStmt) {
                return buildThrow(throwStmt, incoming);
            }
            if (statement instanceof BreakStmt breakStmt) {
                return buildJump(breakStmt, incoming, "BREAK", "break", breakTargets.peek());
            }
            if (statement instanceof ContinueStmt continueStmt) {
                return buildJump(continueStmt, incoming, "CONTINUE", "continue", continueTargets.peek());
            }
            return buildSimpleStatement(statement, incoming);
        }

        private List<PendingEdge> buildIf(IfStmt statement, List<PendingEdge> incoming) {
            ConditionFlow condition = buildCondition(statement.getCondition(), incoming, "CONDITION", "if");
            List<PendingEdge> trueExits = buildStatement(statement.getThenStmt(), condition.trueExits());
            List<PendingEdge> falseExits = statement.getElseStmt()
                    .map(elseStatement -> buildStatement(elseStatement, condition.falseExits()))
                    .orElse(condition.falseExits());
            return join("After if", lineOf(statement), concat(trueExits, falseExits));
        }

        private List<PendingEdge> buildWhile(WhileStmt statement, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After while", lineOf(statement));
            ConditionFlow condition = buildCondition(statement.getCondition(), incoming, "LOOP", "while");

            breakTargets.push(merge.getId());
            continueTargets.push(condition.entryNodeId());
            List<PendingEdge> bodyExits = buildStatement(statement.getBody(), condition.trueExits());
            continueTargets.pop();
            breakTargets.pop();

            connectTo(bodyExits, condition.entryNodeId(), "LOOP_BACK", "loop back");
            connectTo(condition.falseExits(), merge.getId());
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> buildFor(ForStmt statement, List<PendingEdge> incoming) {
            List<PendingEdge> conditionIncoming = incoming;
            if (!statement.getInitialization().isEmpty()) {
                ControlFlowGraph.GraphNode init = addNode(
                        "STATEMENT",
                        "for init: " + statement.getInitialization().stream()
                                .map(Node::toString)
                                .collect(Collectors.joining(", ")),
                        lineOf(statement)
                );
                connectTo(conditionIncoming, init.getId());
                conditionIncoming = List.of(nextFrom(init));
            }

            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After for", lineOf(statement));
            ControlFlowGraph.GraphNode update = null;
            if (!statement.getUpdate().isEmpty()) {
                update = createNode(
                        "STATEMENT",
                        "for update: " + statement.getUpdate().stream()
                                .map(Node::toString)
                                .collect(Collectors.joining(", ")),
                        lineOf(statement)
                );
            }

            ConditionFlow condition;
            if (statement.getCompare().isPresent()) {
                condition = buildCondition(statement.getCompare().get(), conditionIncoming, "LOOP", "for");
            } else {
                condition = buildImplicitLoopCondition(forHeader(statement), lineOf(statement), conditionIncoming);
            }

            breakTargets.push(merge.getId());
            continueTargets.push(update == null ? condition.entryNodeId() : update.getId());
            List<PendingEdge> bodyExits = buildStatement(statement.getBody(), condition.trueExits());
            continueTargets.pop();
            breakTargets.pop();

            if (update != null) {
                nodes.add(update);
                connectTo(bodyExits, update.getId(), "NEXT", "update");
                addEdge(update.getId(), condition.entryNodeId(), "LOOP_BACK", "loop back");
            } else {
                connectTo(bodyExits, condition.entryNodeId(), "LOOP_BACK", "loop back");
            }
            connectTo(condition.falseExits(), merge.getId());
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> buildForEach(ForEachStmt statement, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After foreach", lineOf(statement));
            ConditionFlow condition = buildImplicitLoopCondition(
                    "foreach (" + statement.getVariable() + " : " + statement.getIterable() + ")",
                    lineOf(statement),
                    incoming
            );

            breakTargets.push(merge.getId());
            continueTargets.push(condition.entryNodeId());
            List<PendingEdge> bodyExits = buildStatement(statement.getBody(), condition.trueExits());
            continueTargets.pop();
            breakTargets.pop();

            connectTo(bodyExits, condition.entryNodeId(), "LOOP_BACK", "loop back");
            connectTo(condition.falseExits(), merge.getId());
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> buildDoWhile(DoStmt statement, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode bodyEntry = addNode("STATEMENT", "do body", lineOf(statement));
            connectTo(incoming, bodyEntry.getId());

            ControlFlowGraph.GraphNode condition = createNode(
                    "LOOP",
                    "do while (" + statement.getCondition() + ")",
                    lineOf(statement.getCondition())
            );
            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After do while", lineOf(statement));

            breakTargets.push(merge.getId());
            continueTargets.push(condition.getId());
            List<PendingEdge> bodyExits = buildStatement(statement.getBody(), List.of(nextFrom(bodyEntry)));
            continueTargets.pop();
            breakTargets.pop();

            nodes.add(condition);
            connectTo(bodyExits, condition.getId(), "NEXT", "condition");
            addEdge(condition.getId(), bodyEntry.getId(), "LOOP_BACK", "loop back");
            addEdge(condition.getId(), merge.getId(), "FALSE", "false");
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> buildSwitch(SwitchStmt statement, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode selector = addNode(
                    "CASE",
                    "switch (" + statement.getSelector() + ")",
                    lineOf(statement)
            );
            connectTo(incoming, selector.getId());

            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After switch", lineOf(statement));
            List<PendingEdge> fallthrough = List.of();
            boolean hasDefault = false;

            breakTargets.push(merge.getId());
            for (SwitchEntry entry : statement.getEntries()) {
                hasDefault = hasDefault || entry.getLabels().isEmpty();
                ControlFlowGraph.GraphNode caseNode = addNode("CASE", switchEntryLabel(entry), lineOf(entry));
                connectTo(List.of(new PendingEdge(selector.getId(), "CASE_BRANCH", switchEdgeLabel(entry))), caseNode.getId());
                connectTo(fallthrough, caseNode.getId(), "NEXT", "fall through");
                fallthrough = buildStatements(entry.getStatements(), List.of(nextFrom(caseNode)));
            }
            breakTargets.pop();

            if (!hasDefault) {
                addEdge(selector.getId(), merge.getId(), "FALSE", "no match");
            }
            connectTo(fallthrough, merge.getId());
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> buildTry(TryStmt statement, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode tryNode = addNode("STATEMENT", "try", lineOf(statement));
            connectTo(incoming, tryNode.getId());

            ControlFlowGraph.GraphNode merge = createNode("MERGE", "After try", lineOf(statement));
            List<PendingEdge> exits = new ArrayList<>(buildBlock(statement.getTryBlock(), List.of(nextFrom(tryNode))));

            for (CatchClause catchClause : statement.getCatchClauses()) {
                ControlFlowGraph.GraphNode catchNode = addNode("CATCH", catchLabel(catchClause), lineOf(catchClause));
                addEdge(tryNode.getId(), catchNode.getId(), "EXCEPTION", "catch");
                exits.addAll(buildBlock(catchClause.getBody(), List.of(nextFrom(catchNode))));
            }

            if (statement.getFinallyBlock().isPresent()) {
                exits = new ArrayList<>(buildBlock(statement.getFinallyBlock().get(), exits));
            }

            return joinInto(merge, exits);
        }

        private List<PendingEdge> buildReturn(ReturnStmt statement, List<PendingEdge> incoming) {
            List<PendingEdge> ready = statement.getExpression()
                    .map(expression -> buildExpressionDecisions(expression, incoming))
                    .orElse(incoming);
            ControlFlowGraph.GraphNode node = addNode("STATEMENT", snippet(statement), lineOf(statement));
            connectTo(ready, node.getId());
            addEdge(node.getId(), exit.getId(), "RETURN", "return");
            return List.of();
        }

        private List<PendingEdge> buildThrow(ThrowStmt statement, List<PendingEdge> incoming) {
            List<PendingEdge> ready = buildExpressionDecisions(statement.getExpression(), incoming);
            ControlFlowGraph.GraphNode node = addNode("STATEMENT", snippet(statement), lineOf(statement));
            connectTo(ready, node.getId());
            addEdge(node.getId(), exit.getId(), "THROW", "throw");
            return List.of();
        }

        private List<PendingEdge> buildJump(Statement statement, List<PendingEdge> incoming, String edgeType,
                                            String edgeLabel, String target) {
            ControlFlowGraph.GraphNode node = addNode("STATEMENT", snippet(statement), lineOf(statement));
            connectTo(incoming, node.getId());
            if (target == null) {
                addEdge(node.getId(), exit.getId(), edgeType, edgeLabel);
            } else {
                addEdge(node.getId(), target, edgeType, edgeLabel);
            }
            return List.of();
        }

        private List<PendingEdge> buildSimpleStatement(Statement statement, List<PendingEdge> incoming) {
            List<PendingEdge> ready = buildExpressionDecisions(statement, incoming);
            ControlFlowGraph.GraphNode node = addNode("STATEMENT", snippet(statement), lineOf(statement));
            connectTo(ready, node.getId());
            return List.of(nextFrom(node));
        }

        private ConditionFlow buildCondition(Expression expression, List<PendingEdge> incoming,
                                             String nodeType, String context) {
            Expression normalized = unwrap(expression);
            if (normalized instanceof BinaryExpr binaryExpr && isShortCircuit(binaryExpr)) {
                if (binaryExpr.getOperator() == BinaryExpr.Operator.AND) {
                    ConditionFlow left = buildCondition(binaryExpr.getLeft(), incoming, nodeType, context);
                    ConditionFlow right = buildCondition(binaryExpr.getRight(), left.trueExits(), "CONDITION", "&&");
                    return new ConditionFlow(
                            left.entryNodeId(),
                            right.trueExits(),
                            concat(left.falseExits(), right.falseExits())
                    );
                }
                ConditionFlow left = buildCondition(binaryExpr.getLeft(), incoming, nodeType, context);
                ConditionFlow right = buildCondition(binaryExpr.getRight(), left.falseExits(), "CONDITION", "||");
                return new ConditionFlow(
                        left.entryNodeId(),
                        concat(left.trueExits(), right.trueExits()),
                        right.falseExits()
                );
            }
            if (normalized instanceof ConditionalExpr conditionalExpr) {
                ConditionFlow condition = buildCondition(conditionalExpr.getCondition(), incoming, "CONDITION", "?:");
                ConditionFlow thenFlow = buildCondition(conditionalExpr.getThenExpr(), condition.trueExits(), "CONDITION", "then");
                ConditionFlow elseFlow = buildCondition(conditionalExpr.getElseExpr(), condition.falseExits(), "CONDITION", "else");
                return new ConditionFlow(
                        condition.entryNodeId(),
                        concat(thenFlow.trueExits(), elseFlow.trueExits()),
                        concat(thenFlow.falseExits(), elseFlow.falseExits())
                );
            }

            ControlFlowGraph.GraphNode node = addNode(nodeType, controlLabel(context, normalized), lineOf(normalized));
            connectTo(incoming, node.getId());
            return new ConditionFlow(
                    node.getId(),
                    List.of(new PendingEdge(node.getId(), "TRUE", "true")),
                    List.of(new PendingEdge(node.getId(), "FALSE", "false"))
            );
        }

        private ConditionFlow buildImplicitLoopCondition(String label, int line, List<PendingEdge> incoming) {
            ControlFlowGraph.GraphNode node = addNode("LOOP", label, line);
            connectTo(incoming, node.getId());
            return new ConditionFlow(
                    node.getId(),
                    List.of(new PendingEdge(node.getId(), "TRUE", "true")),
                    List.of(new PendingEdge(node.getId(), "FALSE", "false"))
            );
        }

        private List<PendingEdge> buildExpressionDecisions(Statement statement, List<PendingEdge> incoming) {
            List<ExpressionDecision> decisions = new ArrayList<>();
            statement.findAll(ConditionalExpr.class).stream()
                    .map(expression -> new ExpressionDecision(expression, "?: " + expression.getCondition()))
                    .forEach(decisions::add);
            statement.findAll(BinaryExpr.class).stream()
                    .filter(this::isShortCircuit)
                    .map(expression -> new ExpressionDecision(
                            expression,
                            expression.getOperator().asString() + " " + expression
                    ))
                    .forEach(decisions::add);
            return buildExpressionDecisionChain(decisions, incoming);
        }

        private List<PendingEdge> buildExpressionDecisions(Expression expression, List<PendingEdge> incoming) {
            List<ExpressionDecision> decisions = new ArrayList<>();
            expression.findAll(ConditionalExpr.class).stream()
                    .map(node -> new ExpressionDecision(node, "?: " + node.getCondition()))
                    .forEach(decisions::add);
            expression.findAll(BinaryExpr.class).stream()
                    .filter(this::isShortCircuit)
                    .map(node -> new ExpressionDecision(node, node.getOperator().asString() + " " + node))
                    .forEach(decisions::add);
            return buildExpressionDecisionChain(decisions, incoming);
        }

        private List<PendingEdge> buildExpressionDecisionChain(List<ExpressionDecision> decisions,
                                                              List<PendingEdge> incoming) {
            decisions.sort(Comparator
                    .comparingInt((ExpressionDecision decision) -> lineOf(decision.expression()))
                    .thenComparingInt(decision -> decision.expression()
                            .getRange()
                            .map(range -> range.begin.column)
                            .orElse(0))
                    .thenComparing(ExpressionDecision::label));

            List<PendingEdge> current = incoming;
            for (ExpressionDecision decision : decisions) {
                ControlFlowGraph.GraphNode node = addNode(
                        "CONDITION",
                        decision.label(),
                        lineOf(decision.expression())
                );
                connectTo(current, node.getId());
                current = List.of(
                        new PendingEdge(node.getId(), "TRUE", "true"),
                        new PendingEdge(node.getId(), "FALSE", "false")
                );
            }
            return current;
        }

        private List<PendingEdge> join(String label, int line, List<PendingEdge> incoming) {
            if (incoming.isEmpty()) {
                return List.of();
            }
            ControlFlowGraph.GraphNode merge = addNode("MERGE", label, line);
            connectTo(incoming, merge.getId());
            return List.of(nextFrom(merge));
        }

        private List<PendingEdge> joinInto(ControlFlowGraph.GraphNode merge, List<PendingEdge> incoming) {
            if (incoming.isEmpty()) {
                return List.of();
            }
            connectTo(incoming, merge.getId());
            nodes.add(merge);
            return List.of(nextFrom(merge));
        }

        private void connectTo(List<PendingEdge> incoming, String targetId) {
            for (PendingEdge pendingEdge : incoming) {
                addEdge(pendingEdge.from(), targetId, pendingEdge.type(), pendingEdge.label());
            }
        }

        private void connectTo(List<PendingEdge> incoming, String targetId, String type, String label) {
            for (PendingEdge pendingEdge : incoming) {
                addEdge(pendingEdge.from(), targetId, type, label);
            }
        }

        private PendingEdge nextFrom(ControlFlowGraph.GraphNode node) {
            return nextFrom(node, "next");
        }

        private PendingEdge nextFrom(ControlFlowGraph.GraphNode node, String label) {
            return new PendingEdge(node.getId(), "NEXT", label);
        }

        private ControlFlowGraph.GraphNode addNode(String type, String label, int line) {
            ControlFlowGraph.GraphNode node = createNode(type, label, line);
            nodes.add(node);
            return node;
        }

        private ControlFlowGraph.GraphNode createNode(String type, String label, int line) {
            ControlFlowGraph.GraphNode node = new ControlFlowGraph.GraphNode();
            node.setId("n" + nodeIndex++);
            node.setType(type);
            node.setLabel(compact(label));
            node.setLine(line);
            return node;
        }

        private void addEdge(String from, String to, String type, String label) {
            ControlFlowGraph.GraphEdge edge = new ControlFlowGraph.GraphEdge();
            edge.setId("e" + edgeIndex++);
            edge.setFrom(from);
            edge.setTo(to);
            edge.setType(type);
            edge.setLabel(label);
            edges.add(edge);
        }

        private boolean isShortCircuit(BinaryExpr expression) {
            return expression.getOperator() == BinaryExpr.Operator.AND
                    || expression.getOperator() == BinaryExpr.Operator.OR;
        }

        private Expression unwrap(Expression expression) {
            Expression current = expression;
            while (current.isEnclosedExpr()) {
                current = current.asEnclosedExpr().getInner();
            }
            return current;
        }

        private String controlLabel(String context, Expression expression) {
            return switch (context) {
                case "&&", "||" -> context + " (" + expression + ")";
                case "then" -> "then (" + expression + ")";
                case "else" -> "else (" + expression + ")";
                default -> context + " (" + expression + ")";
            };
        }

        private String forHeader(ForStmt statement) {
            String init = statement.getInitialization().stream()
                    .map(Node::toString)
                    .collect(Collectors.joining(", "));
            String compare = statement.getCompare().map(Node::toString).orElse("");
            String update = statement.getUpdate().stream()
                    .map(Node::toString)
                    .collect(Collectors.joining(", "));
            return "for (" + init + "; " + compare + "; " + update + ")";
        }

        private String switchEntryLabel(SwitchEntry entry) {
            if (entry.getLabels().isEmpty()) {
                return "default";
            }
            return "case " + entry.getLabels().stream()
                    .map(Node::toString)
                    .collect(Collectors.joining(", "));
        }

        private String switchEdgeLabel(SwitchEntry entry) {
            return entry.getLabels().isEmpty() ? "default" : "case";
        }

        private String catchLabel(CatchClause catchClause) {
            return "catch (" + catchClause.getParameter() + ")";
        }

        private String snippet(Node node) {
            return compact(node.toString());
        }

        private String compact(String label) {
            String compacted = label == null ? "" : label.replaceAll("\\s+", " ").trim();
            if (compacted.length() <= MAX_LABEL_LENGTH) {
                return compacted;
            }
            return compacted.substring(0, MAX_LABEL_LENGTH - 1) + "...";
        }

        private int lineOf(Node node) {
            return node.getRange().map(range -> range.begin.line).orElse(0);
        }

        private List<PendingEdge> concat(List<PendingEdge> first, List<PendingEdge> second) {
            if (first.isEmpty()) {
                return second;
            }
            if (second.isEmpty()) {
                return first;
            }
            List<PendingEdge> result = new ArrayList<>(first);
            result.addAll(second);
            return result;
        }

        private record PendingEdge(String from, String type, String label) {
        }

        private record ConditionFlow(String entryNodeId, List<PendingEdge> trueExits,
                                     List<PendingEdge> falseExits) {
        }

        private record ExpressionDecision(Expression expression, String label) {
        }
    }

    private static final class DecisionPointSketchBuild {
        private ControlFlowGraph build(List<DecisionPoint> decisionPoints) {
            List<ControlFlowGraph.GraphNode> nodes = new ArrayList<>();
            List<ControlFlowGraph.GraphEdge> edges = new ArrayList<>();

            ControlFlowGraph.GraphNode entry = node("n0", "ENTRY", "Entry", 0);
            nodes.add(entry);
            String previous = entry.getId();
            int index = 1;
            int edgeIndex = 1;

            if (decisionPoints.isEmpty()) {
                ControlFlowGraph.GraphNode statement = node("n" + index++, "STATEMENT", "Sequential statements", 0);
                nodes.add(statement);
                edges.add(edge("e" + edgeIndex++, previous, statement.getId(), "NEXT", "next"));
                previous = statement.getId();
            }

            for (DecisionPoint decisionPoint : decisionPoints) {
                GraphBuildStep step = buildDecisionStep(decisionPoint, index, edgeIndex, previous);
                nodes.addAll(step.nodes());
                edges.addAll(step.edges());
                index = step.nextNodeIndex();
                edgeIndex = step.nextEdgeIndex();
                previous = step.nextPreviousNodeId();
            }

            ControlFlowGraph.GraphNode exit = node("n" + index, "EXIT", "Exit", 0);
            nodes.add(exit);
            edges.add(edge("e" + edgeIndex, previous, exit.getId(), "NEXT", "exit"));

            ControlFlowGraph graph = new ControlFlowGraph();
            graph.setNodes(nodes);
            graph.setEdges(edges);
            return graph;
        }

        private GraphBuildStep buildDecisionStep(DecisionPoint decisionPoint, int nodeIndex, int edgeIndex,
                                                 String previousNodeId) {
            List<ControlFlowGraph.GraphNode> nodes = new ArrayList<>();
            List<ControlFlowGraph.GraphEdge> edges = new ArrayList<>();

            ControlFlowGraph.GraphNode decision = node(
                    "n" + nodeIndex++,
                    nodeType(decisionPoint.getKind()),
                    decisionPoint.getLabel(),
                    decisionPoint.getLine()
            );
            nodes.add(decision);
            edges.add(edge("e" + edgeIndex++, previousNodeId, decision.getId(), "NEXT", "next"));

            switch (decisionPoint.getKind()) {
                case "FOR", "FOREACH", "WHILE", "DO_WHILE" -> {
                    ControlFlowGraph.GraphNode loopBody = node(
                            "n" + nodeIndex++,
                            "STATEMENT",
                            "Loop body",
                            decisionPoint.getLine()
                    );
                    ControlFlowGraph.GraphNode merge = node(
                            "n" + nodeIndex++,
                            "MERGE",
                            "After loop",
                            decisionPoint.getLine()
                    );
                    nodes.add(loopBody);
                    nodes.add(merge);
                    edges.add(edge("e" + edgeIndex++, decision.getId(), loopBody.getId(), "TRUE", "enter loop"));
                    edges.add(edge("e" + edgeIndex++, decision.getId(), merge.getId(), "FALSE", "exit loop"));
                    edges.add(edge("e" + edgeIndex++, loopBody.getId(), decision.getId(), "LOOP_BACK", "loop back"));
                    return new GraphBuildStep(nodes, edges, nodeIndex, edgeIndex, merge.getId());
                }
                case "SWITCH_CASE" -> {
                    ControlFlowGraph.GraphNode caseBody = node(
                            "n" + nodeIndex++,
                            "STATEMENT",
                            "Case branch",
                            decisionPoint.getLine()
                    );
                    ControlFlowGraph.GraphNode merge = node(
                            "n" + nodeIndex++,
                            "MERGE",
                            "After case",
                            decisionPoint.getLine()
                    );
                    nodes.add(caseBody);
                    nodes.add(merge);
                    edges.add(edge("e" + edgeIndex++, decision.getId(), caseBody.getId(), "CASE_BRANCH", "case"));
                    edges.add(edge("e" + edgeIndex++, caseBody.getId(), merge.getId(), "NEXT", "join"));
                    return new GraphBuildStep(nodes, edges, nodeIndex, edgeIndex, merge.getId());
                }
                case "CATCH" -> {
                    ControlFlowGraph.GraphNode handler = node(
                            "n" + nodeIndex++,
                            "STATEMENT",
                            "Catch handler",
                            decisionPoint.getLine()
                    );
                    ControlFlowGraph.GraphNode merge = node(
                            "n" + nodeIndex++,
                            "MERGE",
                            "After catch",
                            decisionPoint.getLine()
                    );
                    nodes.add(handler);
                    nodes.add(merge);
                    edges.add(edge("e" + edgeIndex++, decision.getId(), handler.getId(), "EXCEPTION", "catch"));
                    edges.add(edge("e" + edgeIndex++, handler.getId(), merge.getId(), "NEXT", "recover"));
                    return new GraphBuildStep(nodes, edges, nodeIndex, edgeIndex, merge.getId());
                }
                default -> {
                    ControlFlowGraph.GraphNode trueBranch = node(
                            "n" + nodeIndex++,
                            "STATEMENT",
                            "True branch",
                            decisionPoint.getLine()
                    );
                    ControlFlowGraph.GraphNode falseBranch = node(
                            "n" + nodeIndex++,
                            "STATEMENT",
                            "False branch",
                            decisionPoint.getLine()
                    );
                    ControlFlowGraph.GraphNode merge = node(
                            "n" + nodeIndex++,
                            "MERGE",
                            "After branch",
                            decisionPoint.getLine()
                    );
                    nodes.add(trueBranch);
                    nodes.add(falseBranch);
                    nodes.add(merge);
                    edges.add(edge("e" + edgeIndex++, decision.getId(), trueBranch.getId(), "TRUE", "true"));
                    edges.add(edge("e" + edgeIndex++, decision.getId(), falseBranch.getId(), "FALSE", "false"));
                    edges.add(edge("e" + edgeIndex++, trueBranch.getId(), merge.getId(), "NEXT", "join"));
                    edges.add(edge("e" + edgeIndex++, falseBranch.getId(), merge.getId(), "NEXT", "join"));
                    return new GraphBuildStep(nodes, edges, nodeIndex, edgeIndex, merge.getId());
                }
            }
        }

        private String nodeType(String decisionKind) {
            return switch (decisionKind) {
                case "FOR", "FOREACH", "WHILE", "DO_WHILE" -> "LOOP";
                case "SWITCH_CASE" -> "CASE";
                case "CATCH" -> "CATCH";
                case "TERNARY", "LOGICAL", "IF" -> "CONDITION";
                default -> "STATEMENT";
            };
        }

        private ControlFlowGraph.GraphNode node(String id, String type, String label, int line) {
            ControlFlowGraph.GraphNode node = new ControlFlowGraph.GraphNode();
            node.setId(id);
            node.setType(type);
            node.setLabel(label);
            node.setLine(line);
            return node;
        }

        private ControlFlowGraph.GraphEdge edge(String id, String from, String to, String type, String label) {
            ControlFlowGraph.GraphEdge edge = new ControlFlowGraph.GraphEdge();
            edge.setId(id);
            edge.setFrom(from);
            edge.setTo(to);
            edge.setType(type);
            edge.setLabel(label);
            return edge;
        }

        private record GraphBuildStep(
                List<ControlFlowGraph.GraphNode> nodes,
                List<ControlFlowGraph.GraphEdge> edges,
                int nextNodeIndex,
                int nextEdgeIndex,
                String nextPreviousNodeId
        ) {
        }
    }
}
