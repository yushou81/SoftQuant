package com.softquant.backend.metrics.cfg.service;

import com.softquant.backend.metrics.cfg.dto.ControlFlowGraph;
import com.softquant.backend.metrics.cfg.dto.DecisionPoint;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ControlFlowGraphBuilder {

    public ControlFlowGraph build(List<DecisionPoint> decisionPoints) {
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
            ControlFlowGraph.GraphNode decision = node(
                    "n" + index++,
                    nodeType(decisionPoint.getKind()),
                    decisionPoint.getLabel(),
                    decisionPoint.getLine()
            );
            ControlFlowGraph.GraphNode merge = node(
                    "n" + index++,
                    "MERGE",
                    "Merge after " + decisionPoint.getLabel(),
                    decisionPoint.getLine()
            );
            nodes.add(decision);
            nodes.add(merge);
            edges.add(edge("e" + edgeIndex++, previous, decision.getId(), "NEXT", "next"));
            addDecisionEdges(edges, "e" + edgeIndex, decisionPoint, decision.getId(), merge.getId());
            edgeIndex += 2;
            previous = merge.getId();
        }

        ControlFlowGraph.GraphNode exit = node("n" + index, "EXIT", "Exit", 0);
        nodes.add(exit);
        edges.add(edge("e" + edgeIndex, previous, exit.getId(), "NEXT", "exit"));

        ControlFlowGraph graph = new ControlFlowGraph();
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    private void addDecisionEdges(
            List<ControlFlowGraph.GraphEdge> edges,
            String firstEdgeId,
            DecisionPoint decisionPoint,
            String from,
            String to
    ) {
        int edgeNumber = Integer.parseInt(firstEdgeId.substring(1));
        switch (decisionPoint.getKind()) {
            case "FOR", "FOREACH", "WHILE", "DO_WHILE" -> {
                edges.add(edge("e" + edgeNumber, from, to, "FALSE", "exit loop"));
                edges.add(edge("e" + (edgeNumber + 1), from, from, "LOOP_BACK", "loop back"));
            }
            case "SWITCH_CASE" -> {
                edges.add(edge("e" + edgeNumber, from, to, "CASE_BRANCH", "case"));
                edges.add(edge("e" + (edgeNumber + 1), from, to, "NEXT", "fall through"));
            }
            case "CATCH" -> {
                edges.add(edge("e" + edgeNumber, from, to, "EXCEPTION", "catch"));
                edges.add(edge("e" + (edgeNumber + 1), from, to, "NEXT", "try path"));
            }
            default -> {
                edges.add(edge("e" + edgeNumber, from, to, "TRUE", "true"));
                edges.add(edge("e" + (edgeNumber + 1), from, to, "FALSE", "false"));
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
}
