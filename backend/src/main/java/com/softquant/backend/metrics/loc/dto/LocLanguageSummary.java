package com.softquant.backend.metrics.loc.dto;

import java.math.BigDecimal;

public class LocLanguageSummary {

    private String language;
    private int fileCount;
    private int physicalLines;
    private int logicalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;
    private int mixedLines;
    private BigDecimal commentRate;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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
}
