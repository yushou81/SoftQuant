package com.softquant.backend.metrics.ck.dto;

import java.util.List;

public class CkAnalysisResponse {

    private String projectName;
    private int classCount;
    private int avgWmc;
    private int avgDit;
    private int avgNoc;
    private int avgCbo;
    private int avgRfc;
    private int avgLcom;
    private List<CkClassMetrics> classes;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getAvgWmc() {
        return avgWmc;
    }

    public void setAvgWmc(int avgWmc) {
        this.avgWmc = avgWmc;
    }

    public int getAvgCbo() {
        return avgCbo;
    }

    public void setAvgCbo(int avgCbo) {
        this.avgCbo = avgCbo;
    }

    public int getAvgDit() {
        return avgDit;
    }

    public void setAvgDit(int avgDit) {
        this.avgDit = avgDit;
    }

    public int getAvgNoc() {
        return avgNoc;
    }

    public void setAvgNoc(int avgNoc) {
        this.avgNoc = avgNoc;
    }

    public int getAvgRfc() {
        return avgRfc;
    }

    public void setAvgRfc(int avgRfc) {
        this.avgRfc = avgRfc;
    }

    public int getAvgLcom() {
        return avgLcom;
    }

    public void setAvgLcom(int avgLcom) {
        this.avgLcom = avgLcom;
    }

    public List<CkClassMetrics> getClasses() {
        return classes;
    }

    public void setClasses(List<CkClassMetrics> classes) {
        this.classes = classes;
    }
}
