package com.softquant.backend.metrics.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class AnalysisRequest {

    private String projectName;
    private String metricSet;
    private String classDiagramText;
    private String flowDiagramText;
    private String useCaseText;
    private Integer teamSize;
    private Integer estimateWeeks;

    @Valid
    @NotEmpty(message = "sources must not be empty")
    private List<JavaSourceInput> sources;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getMetricSet() {
        return metricSet;
    }

    public void setMetricSet(String metricSet) {
        this.metricSet = metricSet;
    }

    public List<JavaSourceInput> getSources() {
        return sources;
    }

    public void setSources(List<JavaSourceInput> sources) {
        this.sources = sources;
    }

    public String getClassDiagramText() {
        return classDiagramText;
    }

    public void setClassDiagramText(String classDiagramText) {
        this.classDiagramText = classDiagramText;
    }

    public String getFlowDiagramText() {
        return flowDiagramText;
    }

    public void setFlowDiagramText(String flowDiagramText) {
        this.flowDiagramText = flowDiagramText;
    }

    public String getUseCaseText() {
        return useCaseText;
    }

    public void setUseCaseText(String useCaseText) {
        this.useCaseText = useCaseText;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Integer getEstimateWeeks() {
        return estimateWeeks;
    }

    public void setEstimateWeeks(Integer estimateWeeks) {
        this.estimateWeeks = estimateWeeks;
    }
}
