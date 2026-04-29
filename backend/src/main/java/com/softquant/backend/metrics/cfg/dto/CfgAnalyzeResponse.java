package com.softquant.backend.metrics.cfg.dto;

import java.math.BigDecimal;
import java.util.List;

public class CfgAnalyzeResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private int fileCount;
    private int classCount;
    private int methodCount;
    private BigDecimal avgComplexity;
    private int maxComplexity;
    private int lowRiskMethodCount;
    private int mediumRiskMethodCount;
    private int highRiskMethodCount;
    private int veryHighRiskMethodCount;
    private List<MethodComplexityMetrics> methods;
    private List<ClassComplexitySummary> classSummaries;
    private List<String> parseIssues;
    private List<TraceLine> formulaTrace;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getContractKind() {
        return contractKind;
    }

    public void setContractKind(String contractKind) {
        this.contractKind = contractKind;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public BigDecimal getAvgComplexity() {
        return avgComplexity;
    }

    public void setAvgComplexity(BigDecimal avgComplexity) {
        this.avgComplexity = avgComplexity;
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public void setMaxComplexity(int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    public int getLowRiskMethodCount() {
        return lowRiskMethodCount;
    }

    public void setLowRiskMethodCount(int lowRiskMethodCount) {
        this.lowRiskMethodCount = lowRiskMethodCount;
    }

    public int getMediumRiskMethodCount() {
        return mediumRiskMethodCount;
    }

    public void setMediumRiskMethodCount(int mediumRiskMethodCount) {
        this.mediumRiskMethodCount = mediumRiskMethodCount;
    }

    public int getHighRiskMethodCount() {
        return highRiskMethodCount;
    }

    public void setHighRiskMethodCount(int highRiskMethodCount) {
        this.highRiskMethodCount = highRiskMethodCount;
    }

    public int getVeryHighRiskMethodCount() {
        return veryHighRiskMethodCount;
    }

    public void setVeryHighRiskMethodCount(int veryHighRiskMethodCount) {
        this.veryHighRiskMethodCount = veryHighRiskMethodCount;
    }

    public List<MethodComplexityMetrics> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodComplexityMetrics> methods) {
        this.methods = methods;
    }

    public List<ClassComplexitySummary> getClassSummaries() {
        return classSummaries;
    }

    public void setClassSummaries(List<ClassComplexitySummary> classSummaries) {
        this.classSummaries = classSummaries;
    }

    public List<String> getParseIssues() {
        return parseIssues;
    }

    public void setParseIssues(List<String> parseIssues) {
        this.parseIssues = parseIssues;
    }

    public List<TraceLine> getFormulaTrace() {
        return formulaTrace;
    }

    public void setFormulaTrace(List<TraceLine> formulaTrace) {
        this.formulaTrace = formulaTrace;
    }

    public static class TraceLine {
        private String label;
        private String expression;
        private String result;

        public TraceLine() {
        }

        public TraceLine(String label, String expression, String result) {
            this.label = label;
            this.expression = expression;
            this.result = result;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
