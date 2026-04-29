package com.softquant.backend.metrics.loc.dto;

import java.math.BigDecimal;
import java.util.List;

public class LocAnalyzeResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private int fileCount;
    private int physicalLines;
    private int logicalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;
    private int mixedLines;
    private BigDecimal commentRate;
    private List<LocFileMetrics> files;
    private List<LocLanguageSummary> languageSummaries;
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

    public int getPhysicalLines() {
        return physicalLines;
    }

    public void setPhysicalLines(int physicalLines) {
        this.physicalLines = physicalLines;
    }

    public int getLogicalLines() {
        return logicalLines;
    }

    public void setLogicalLines(int logicalLines) {
        this.logicalLines = logicalLines;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public void setCodeLines(int codeLines) {
        this.codeLines = codeLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public void setCommentLines(int commentLines) {
        this.commentLines = commentLines;
    }

    public int getBlankLines() {
        return blankLines;
    }

    public void setBlankLines(int blankLines) {
        this.blankLines = blankLines;
    }

    public int getMixedLines() {
        return mixedLines;
    }

    public void setMixedLines(int mixedLines) {
        this.mixedLines = mixedLines;
    }

    public BigDecimal getCommentRate() {
        return commentRate;
    }

    public void setCommentRate(BigDecimal commentRate) {
        this.commentRate = commentRate;
    }

    public List<LocFileMetrics> getFiles() {
        return files;
    }

    public void setFiles(List<LocFileMetrics> files) {
        this.files = files;
    }

    public List<LocLanguageSummary> getLanguageSummaries() {
        return languageSummaries;
    }

    public void setLanguageSummaries(List<LocLanguageSummary> languageSummaries) {
        this.languageSummaries = languageSummaries;
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
