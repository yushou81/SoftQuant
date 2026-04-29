package com.softquant.backend.metrics.cfg.dto;

import java.math.BigDecimal;

public class ClassComplexitySummary {

    private String fileName;
    private String className;
    private int methodCount;
    private int totalComplexity;
    private BigDecimal avgComplexity;
    private int maxComplexity;
    private int highRiskMethodCount;

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

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public int getTotalComplexity() {
        return totalComplexity;
    }

    public void setTotalComplexity(int totalComplexity) {
        this.totalComplexity = totalComplexity;
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

    public int getHighRiskMethodCount() {
        return highRiskMethodCount;
    }

    public void setHighRiskMethodCount(int highRiskMethodCount) {
        this.highRiskMethodCount = highRiskMethodCount;
    }
}
