package com.softquant.backend.metrics.common.dto;

import java.util.List;

public class AnalysisResponse {

    private String projectName;
    private int classCount;
    private int avgWmc;
    private int avgDit;
    private int avgNoc;
    private int avgCbo;
    private int avgRfc;
    private int avgLcom;
    private int avgCs;
    private int avgNpa;
    private int avgNoo;
    private int avgNoa;
    private int classCoverage;
    private int methodDriftRate;
    private int inheritanceConsistency;
    private List<String> consistencyIssues;
    private List<ClassMetrics> classes;

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

    public int getAvgCbo() {
        return avgCbo;
    }

    public void setAvgCbo(int avgCbo) {
        this.avgCbo = avgCbo;
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

    public int getAvgCs() {
        return avgCs;
    }

    public void setAvgCs(int avgCs) {
        this.avgCs = avgCs;
    }

    public int getAvgNpa() {
        return avgNpa;
    }

    public void setAvgNpa(int avgNpa) {
        this.avgNpa = avgNpa;
    }

    public int getAvgNoo() {
        return avgNoo;
    }

    public void setAvgNoo(int avgNoo) {
        this.avgNoo = avgNoo;
    }

    public int getAvgNoa() {
        return avgNoa;
    }

    public void setAvgNoa(int avgNoa) {
        this.avgNoa = avgNoa;
    }

    public int getClassCoverage() {
        return classCoverage;
    }

    public void setClassCoverage(int classCoverage) {
        this.classCoverage = classCoverage;
    }

    public int getMethodDriftRate() {
        return methodDriftRate;
    }

    public void setMethodDriftRate(int methodDriftRate) {
        this.methodDriftRate = methodDriftRate;
    }

    public int getInheritanceConsistency() {
        return inheritanceConsistency;
    }

    public void setInheritanceConsistency(int inheritanceConsistency) {
        this.inheritanceConsistency = inheritanceConsistency;
    }

    public List<String> getConsistencyIssues() {
        return consistencyIssues;
    }

    public void setConsistencyIssues(List<String> consistencyIssues) {
        this.consistencyIssues = consistencyIssues;
    }

    public List<ClassMetrics> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassMetrics> classes) {
        this.classes = classes;
    }
}
