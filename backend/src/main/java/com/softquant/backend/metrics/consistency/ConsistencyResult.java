package com.softquant.backend.metrics.consistency;

import java.util.ArrayList;
import java.util.List;

public class ConsistencyResult {

    private int classCoverage;
    private int methodDriftRate;
    private int inheritanceConsistency;
    private List<String> issues = new ArrayList<>();

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

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }
}
