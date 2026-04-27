package com.softquant.backend.metrics.ucp.dto;

import java.math.BigDecimal;
import java.util.List;

public class UcpCalculateResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private BigDecimal uaw;
    private BigDecimal uuc;
    private BigDecimal uucp;
    private BigDecimal tcf;
    private BigDecimal ef;
    private BigDecimal ucp;
    private BigDecimal effort;
    private BigDecimal productivity;
    private String productivityUnit;
    private List<ActorWeightBreakdown> actorWeightBreakdown;
    private List<UseCaseWeightBreakdown> useCaseWeightBreakdown;
    private List<FactorBreakdown> tcfBreakdown;
    private List<FactorBreakdown> efBreakdown;
    private List<FormulaTrace> formulaTrace;
    private List<ExplanationLine> explanations;
    private UcpParseResponse.UcpProcessDetails processDetails;

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

    public BigDecimal getUaw() {
        return uaw;
    }

    public void setUaw(BigDecimal uaw) {
        this.uaw = uaw;
    }

    public BigDecimal getUuc() {
        return uuc;
    }

    public void setUuc(BigDecimal uuc) {
        this.uuc = uuc;
    }

    public BigDecimal getUucp() {
        return uucp;
    }

    public void setUucp(BigDecimal uucp) {
        this.uucp = uucp;
    }

    public BigDecimal getTcf() {
        return tcf;
    }

    public void setTcf(BigDecimal tcf) {
        this.tcf = tcf;
    }

    public BigDecimal getEf() {
        return ef;
    }

    public void setEf(BigDecimal ef) {
        this.ef = ef;
    }

    public BigDecimal getUcp() {
        return ucp;
    }

    public void setUcp(BigDecimal ucp) {
        this.ucp = ucp;
    }

    public BigDecimal getEffort() {
        return effort;
    }

    public void setEffort(BigDecimal effort) {
        this.effort = effort;
    }

    public BigDecimal getProductivity() {
        return productivity;
    }

    public void setProductivity(BigDecimal productivity) {
        this.productivity = productivity;
    }

    public String getProductivityUnit() {
        return productivityUnit;
    }

    public void setProductivityUnit(String productivityUnit) {
        this.productivityUnit = productivityUnit;
    }

    public List<ActorWeightBreakdown> getActorWeightBreakdown() {
        return actorWeightBreakdown;
    }

    public void setActorWeightBreakdown(List<ActorWeightBreakdown> actorWeightBreakdown) {
        this.actorWeightBreakdown = actorWeightBreakdown;
    }

    public List<UseCaseWeightBreakdown> getUseCaseWeightBreakdown() {
        return useCaseWeightBreakdown;
    }

    public void setUseCaseWeightBreakdown(List<UseCaseWeightBreakdown> useCaseWeightBreakdown) {
        this.useCaseWeightBreakdown = useCaseWeightBreakdown;
    }

    public List<FactorBreakdown> getTcfBreakdown() {
        return tcfBreakdown;
    }

    public void setTcfBreakdown(List<FactorBreakdown> tcfBreakdown) {
        this.tcfBreakdown = tcfBreakdown;
    }

    public List<FactorBreakdown> getEfBreakdown() {
        return efBreakdown;
    }

    public void setEfBreakdown(List<FactorBreakdown> efBreakdown) {
        this.efBreakdown = efBreakdown;
    }

    public List<FormulaTrace> getFormulaTrace() {
        return formulaTrace;
    }

    public void setFormulaTrace(List<FormulaTrace> formulaTrace) {
        this.formulaTrace = formulaTrace;
    }

    public List<ExplanationLine> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<ExplanationLine> explanations) {
        this.explanations = explanations;
    }

    public UcpParseResponse.UcpProcessDetails getProcessDetails() {
        return processDetails;
    }

    public void setProcessDetails(UcpParseResponse.UcpProcessDetails processDetails) {
        this.processDetails = processDetails;
    }

    public static class ActorWeightBreakdown {
        private String actorId;
        private String actorName;
        private String selectedComplexity;
        private Integer selectedWeight;
        private BigDecimal subtotal;

        public String getActorId() {
            return actorId;
        }

        public void setActorId(String actorId) {
            this.actorId = actorId;
        }

        public String getActorName() {
            return actorName;
        }

        public void setActorName(String actorName) {
            this.actorName = actorName;
        }

        public String getSelectedComplexity() {
            return selectedComplexity;
        }

        public void setSelectedComplexity(String selectedComplexity) {
            this.selectedComplexity = selectedComplexity;
        }

        public Integer getSelectedWeight() {
            return selectedWeight;
        }

        public void setSelectedWeight(Integer selectedWeight) {
            this.selectedWeight = selectedWeight;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    public static class UseCaseWeightBreakdown {
        private String useCaseId;
        private String useCaseName;
        private String selectedComplexity;
        private Integer selectedWeight;
        private Integer entityCount;
        private Integer stepCount;
        private Integer classCount;
        private BigDecimal subtotal;

        public String getUseCaseId() {
            return useCaseId;
        }

        public void setUseCaseId(String useCaseId) {
            this.useCaseId = useCaseId;
        }

        public String getUseCaseName() {
            return useCaseName;
        }

        public void setUseCaseName(String useCaseName) {
            this.useCaseName = useCaseName;
        }

        public String getSelectedComplexity() {
            return selectedComplexity;
        }

        public void setSelectedComplexity(String selectedComplexity) {
            this.selectedComplexity = selectedComplexity;
        }

        public Integer getSelectedWeight() {
            return selectedWeight;
        }

        public void setSelectedWeight(Integer selectedWeight) {
            this.selectedWeight = selectedWeight;
        }

        public Integer getEntityCount() {
            return entityCount;
        }

        public void setEntityCount(Integer entityCount) {
            this.entityCount = entityCount;
        }

        public Integer getStepCount() {
            return stepCount;
        }

        public void setStepCount(Integer stepCount) {
            this.stepCount = stepCount;
        }

        public Integer getClassCount() {
            return classCount;
        }

        public void setClassCount(Integer classCount) {
            this.classCount = classCount;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    public static class FactorBreakdown {
        private String code;
        private String label;
        private BigDecimal weight;
        private Integer score;
        private BigDecimal weightedScore;

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

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public BigDecimal getWeightedScore() {
            return weightedScore;
        }

        public void setWeightedScore(BigDecimal weightedScore) {
            this.weightedScore = weightedScore;
        }
    }

    public static class FormulaTrace {
        private String formulaKey;
        private String expression;
        private String resultField;
        private BigDecimal result;

        public String getFormulaKey() {
            return formulaKey;
        }

        public void setFormulaKey(String formulaKey) {
            this.formulaKey = formulaKey;
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

    public static class ExplanationLine {
        private String code;
        private String text;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
