package com.softquant.backend.metrics.cfg.dto;

import java.util.List;

public class MethodComplexityMetrics {

    private String fileName;
    private String className;
    private String methodName;
    private String signature;
    private String methodType;
    private int startLine;
    private int endLine;
    private int complexity;
    private String riskLevel;
    private int decisionPointCount;
    private List<DecisionPoint> decisionPoints;
    private ControlFlowGraph graph;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getDecisionPointCount() {
        return decisionPointCount;
    }

    public void setDecisionPointCount(int decisionPointCount) {
        this.decisionPointCount = decisionPointCount;
    }

    public List<DecisionPoint> getDecisionPoints() {
        return decisionPoints;
    }

    public void setDecisionPoints(List<DecisionPoint> decisionPoints) {
        this.decisionPoints = decisionPoints;
    }

    public ControlFlowGraph getGraph() {
        return graph;
    }

    public void setGraph(ControlFlowGraph graph) {
        this.graph = graph;
    }
}
