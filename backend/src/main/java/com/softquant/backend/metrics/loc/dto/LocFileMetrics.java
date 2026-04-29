package com.softquant.backend.metrics.loc.dto;

import java.math.BigDecimal;

public class LocFileMetrics {

    private String fileName;
    private String language;
    private int physicalLines;
    private int logicalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;
    private int mixedLines;
    private BigDecimal commentRate;
    private String parseStatus;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }
}
