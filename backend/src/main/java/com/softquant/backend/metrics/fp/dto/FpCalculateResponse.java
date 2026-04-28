package com.softquant.backend.metrics.fp.dto;

import java.math.BigDecimal;
import java.util.List;

public class FpCalculateResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private BigDecimal ufp;
    private BigDecimal vaf;
    private BigDecimal fp;
    private Integer locEstimate;
    private LanguageProfile language;
    private List<ComponentBreakdown> componentBreakdown;
    private List<ComplexityMatrixHit> complexityMatrixHit;
    private List<GscBreakdown> gscBreakdown;
    private List<TraceLine> ufpTrace;
    private List<TraceLine> vafTrace;
    private List<TraceLine> locTrace;
    private FpParseResponse.ProcessDetails processDetails;

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

    public BigDecimal getUfp() {
        return ufp;
    }

    public void setUfp(BigDecimal ufp) {
        this.ufp = ufp;
    }

    public BigDecimal getVaf() {
        return vaf;
    }

    public void setVaf(BigDecimal vaf) {
        this.vaf = vaf;
    }

    public BigDecimal getFp() {
        return fp;
    }

    public void setFp(BigDecimal fp) {
        this.fp = fp;
    }

    public Integer getLocEstimate() {
        return locEstimate;
    }

    public void setLocEstimate(Integer locEstimate) {
        this.locEstimate = locEstimate;
    }

    public LanguageProfile getLanguage() {
        return language;
    }

    public void setLanguage(LanguageProfile language) {
        this.language = language;
    }

    public List<ComponentBreakdown> getComponentBreakdown() {
        return componentBreakdown;
    }

    public void setComponentBreakdown(List<ComponentBreakdown> componentBreakdown) {
        this.componentBreakdown = componentBreakdown;
    }

    public List<ComplexityMatrixHit> getComplexityMatrixHit() {
        return complexityMatrixHit;
    }

    public void setComplexityMatrixHit(List<ComplexityMatrixHit> complexityMatrixHit) {
        this.complexityMatrixHit = complexityMatrixHit;
    }

    public List<GscBreakdown> getGscBreakdown() {
        return gscBreakdown;
    }

    public void setGscBreakdown(List<GscBreakdown> gscBreakdown) {
        this.gscBreakdown = gscBreakdown;
    }

    public List<TraceLine> getUfpTrace() {
        return ufpTrace;
    }

    public void setUfpTrace(List<TraceLine> ufpTrace) {
        this.ufpTrace = ufpTrace;
    }

    public List<TraceLine> getVafTrace() {
        return vafTrace;
    }

    public void setVafTrace(List<TraceLine> vafTrace) {
        this.vafTrace = vafTrace;
    }

    public List<TraceLine> getLocTrace() {
        return locTrace;
    }

    public void setLocTrace(List<TraceLine> locTrace) {
        this.locTrace = locTrace;
    }

    public FpParseResponse.ProcessDetails getProcessDetails() {
        return processDetails;
    }

    public void setProcessDetails(FpParseResponse.ProcessDetails processDetails) {
        this.processDetails = processDetails;
    }

    public static class LanguageProfile {
        private String code;
        private String label;
        private Integer slocPerFp;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getSlocPerFp() {
            return slocPerFp;
        }

        public void setSlocPerFp(Integer slocPerFp) {
            this.slocPerFp = slocPerFp;
        }
    }

    public static class ComponentBreakdown {
        private String candidateId;
        private String componentType;
        private String name;
        private String complexityLevel;
        private Integer det;
        private Integer ret;
        private Integer ftr;
        private Integer weight;

        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getComponentType() {
            return componentType;
        }

        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getComplexityLevel() {
            return complexityLevel;
        }

        public void setComplexityLevel(String complexityLevel) {
            this.complexityLevel = complexityLevel;
        }

        public Integer getDet() {
            return det;
        }

        public void setDet(Integer det) {
            this.det = det;
        }

        public Integer getRet() {
            return ret;
        }

        public void setRet(Integer ret) {
            this.ret = ret;
        }

        public Integer getFtr() {
            return ftr;
        }

        public void setFtr(Integer ftr) {
            this.ftr = ftr;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }
    }

    public static class ComplexityMatrixHit {
        private String candidateId;
        private String componentType;
        private String complexityLevel;
        private Integer det;
        private Integer ret;
        private Integer ftr;

        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getComponentType() {
            return componentType;
        }

        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }

        public String getComplexityLevel() {
            return complexityLevel;
        }

        public void setComplexityLevel(String complexityLevel) {
            this.complexityLevel = complexityLevel;
        }

        public Integer getDet() {
            return det;
        }

        public void setDet(Integer det) {
            this.det = det;
        }

        public Integer getRet() {
            return ret;
        }

        public void setRet(Integer ret) {
            this.ret = ret;
        }

        public Integer getFtr() {
            return ftr;
        }

        public void setFtr(Integer ftr) {
            this.ftr = ftr;
        }
    }

    public static class GscBreakdown {
        private String code;
        private String label;
        private Integer score;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }

    public static class TraceLine {
        private String key;
        private String expression;
        private String resultField;
        private BigDecimal result;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public String getResultField() {
            return resultField;
        }

        public void setResultField(String resultField) {
            this.resultField = resultField;
        }

        public BigDecimal getResult() {
            return result;
        }

        public void setResult(BigDecimal result) {
            this.result = result;
        }
    }
}
