package com.softquant.backend.metrics.ucp.dto;

import java.util.List;

public class UcpParseResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private String sourceName;
    private String sourceType;
    private List<UcpActorPreview> actors;
    private List<UcpUseCasePreview> useCases;
    private List<UcpRelationshipPreview> relationships;
    private List<UcpPendingField> pendingFields;
    private List<UcpEvidence> evidence;
    private UcpProcessDetails processDetails;

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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public List<UcpActorPreview> getActors() {
        return actors;
    }

    public void setActors(List<UcpActorPreview> actors) {
        this.actors = actors;
    }

    public List<UcpUseCasePreview> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<UcpUseCasePreview> useCases) {
        this.useCases = useCases;
    }

    public List<UcpRelationshipPreview> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<UcpRelationshipPreview> relationships) {
        this.relationships = relationships;
    }

    public List<UcpPendingField> getPendingFields() {
        return pendingFields;
    }

    public void setPendingFields(List<UcpPendingField> pendingFields) {
        this.pendingFields = pendingFields;
    }

    public List<UcpEvidence> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<UcpEvidence> evidence) {
        this.evidence = evidence;
    }

    public UcpProcessDetails getProcessDetails() {
        return processDetails;
    }

    public void setProcessDetails(UcpProcessDetails processDetails) {
        this.processDetails = processDetails;
    }

    public static class UcpActorPreview {
        private String actorId;
        private String actorName;
        private String stereotype;
        private String suggestedComplexity;
        private Integer suggestedWeight;
        private List<String> evidenceCodes;

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

        public String getStereotype() {
            return stereotype;
        }

        public void setStereotype(String stereotype) {
            this.stereotype = stereotype;
        }

        public String getSuggestedComplexity() {
            return suggestedComplexity;
        }

        public void setSuggestedComplexity(String suggestedComplexity) {
            this.suggestedComplexity = suggestedComplexity;
        }

        public Integer getSuggestedWeight() {
            return suggestedWeight;
        }

        public void setSuggestedWeight(Integer suggestedWeight) {
            this.suggestedWeight = suggestedWeight;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class UcpUseCasePreview {
        private String useCaseId;
        private String useCaseName;
        private Integer entityCount;
        private Integer stepCount;
        private Integer classCount;
        private String suggestedComplexity;
        private Integer suggestedWeight;
        private List<String> evidenceCodes;

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

        public String getSuggestedComplexity() {
            return suggestedComplexity;
        }

        public void setSuggestedComplexity(String suggestedComplexity) {
            this.suggestedComplexity = suggestedComplexity;
        }

        public Integer getSuggestedWeight() {
            return suggestedWeight;
        }

        public void setSuggestedWeight(Integer suggestedWeight) {
            this.suggestedWeight = suggestedWeight;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class UcpRelationshipPreview {
        private String relationshipId;
        private String relationshipType;
        private String sourceId;
        private String sourceName;
        private String targetId;
        private String targetName;
        private List<String> evidenceCodes;

        public String getRelationshipId() {
            return relationshipId;
        }

        public void setRelationshipId(String relationshipId) {
            this.relationshipId = relationshipId;
        }

        public String getRelationshipType() {
            return relationshipType;
        }

        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getSourceName() {
            return sourceName;
        }

        public void setSourceName(String sourceName) {
            this.sourceName = sourceName;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class UcpPendingField {
        private String fieldKey;
        private String entityId;
        private String entityName;
        private String reason;

        public String getFieldKey() {
            return fieldKey;
        }

        public void setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class UcpEvidence {
        private String code;
        private String evidenceType;
        private String summary;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getEvidenceType() {
            return evidenceType;
        }

        public void setEvidenceType(String evidenceType) {
            this.evidenceType = evidenceType;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    public static class UcpProcessDetails {
        private List<UcpStepCard> cards;
        private List<UcpTableMeta> tables;

        public List<UcpStepCard> getCards() {
            return cards;
        }

        public void setCards(List<UcpStepCard> cards) {
            this.cards = cards;
        }

        public List<UcpTableMeta> getTables() {
            return tables;
        }

        public void setTables(List<UcpTableMeta> tables) {
            this.tables = tables;
        }
    }

    public static class UcpStepCard {
        private String stepKey;
        private String title;
        private String description;

        public String getStepKey() {
            return stepKey;
        }

        public void setStepKey(String stepKey) {
            this.stepKey = stepKey;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class UcpTableMeta {
        private String tableKey;
        private String title;
        private List<UcpColumnMeta> columns;

        public String getTableKey() {
            return tableKey;
        }

        public void setTableKey(String tableKey) {
            this.tableKey = tableKey;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<UcpColumnMeta> getColumns() {
            return columns;
        }

        public void setColumns(List<UcpColumnMeta> columns) {
            this.columns = columns;
        }
    }

    public static class UcpColumnMeta {
        private String key;
        private String label;
        private String valueType;
        private boolean required;
        private boolean editable;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValueType() {
            return valueType;
        }

        public void setValueType(String valueType) {
            this.valueType = valueType;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }
    }
}
