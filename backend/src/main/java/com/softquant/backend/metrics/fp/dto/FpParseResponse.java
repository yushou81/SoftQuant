package com.softquant.backend.metrics.fp.dto;

import java.util.List;

public class FpParseResponse {

    private String module;
    private String contractKind;
    private String projectName;
    private String sourceName;
    private String sourceType;
    private List<DfdProcessPreview> processes;
    private List<ExternalEntityPreview> externalEntities;
    private List<DataStorePreview> dataStores;
    private List<DataElementPreview> dataElements;
    private List<FlowPreview> flows;
    private List<ComponentCandidate> componentCandidates;
    private List<PendingField> pendingFields;
    private List<EvidenceLine> evidence;
    private ProcessDetails processDetails;

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

    public List<DfdProcessPreview> getProcesses() {
        return processes;
    }

    public void setProcesses(List<DfdProcessPreview> processes) {
        this.processes = processes;
    }

    public List<ExternalEntityPreview> getExternalEntities() {
        return externalEntities;
    }

    public void setExternalEntities(List<ExternalEntityPreview> externalEntities) {
        this.externalEntities = externalEntities;
    }

    public List<DataStorePreview> getDataStores() {
        return dataStores;
    }

    public void setDataStores(List<DataStorePreview> dataStores) {
        this.dataStores = dataStores;
    }

    public List<DataElementPreview> getDataElements() {
        return dataElements;
    }

    public void setDataElements(List<DataElementPreview> dataElements) {
        this.dataElements = dataElements;
    }

    public List<FlowPreview> getFlows() {
        return flows;
    }

    public void setFlows(List<FlowPreview> flows) {
        this.flows = flows;
    }

    public List<ComponentCandidate> getComponentCandidates() {
        return componentCandidates;
    }

    public void setComponentCandidates(List<ComponentCandidate> componentCandidates) {
        this.componentCandidates = componentCandidates;
    }

    public List<PendingField> getPendingFields() {
        return pendingFields;
    }

    public void setPendingFields(List<PendingField> pendingFields) {
        this.pendingFields = pendingFields;
    }

    public List<EvidenceLine> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceLine> evidence) {
        this.evidence = evidence;
    }

    public ProcessDetails getProcessDetails() {
        return processDetails;
    }

    public void setProcessDetails(ProcessDetails processDetails) {
        this.processDetails = processDetails;
    }

    public static class DfdProcessPreview {
        private String processId;
        private String processName;
        private String numberId;
        private List<String> evidenceCodes;

        public String getProcessId() {
            return processId;
        }

        public void setProcessId(String processId) {
            this.processId = processId;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String getNumberId() {
            return numberId;
        }

        public void setNumberId(String numberId) {
            this.numberId = numberId;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class ExternalEntityPreview {
        private String entityId;
        private String entityName;
        private String stereotype;
        private List<String> evidenceCodes;

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

        public String getStereotype() {
            return stereotype;
        }

        public void setStereotype(String stereotype) {
            this.stereotype = stereotype;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class DataStorePreview {
        private String storeId;
        private String storeName;
        private String stereotype;
        private List<String> evidenceCodes;

        public String getStoreId() {
            return storeId;
        }

        public void setStoreId(String storeId) {
            this.storeId = storeId;
        }

        public String getStoreName() {
            return storeName;
        }

        public void setStoreName(String storeName) {
            this.storeName = storeName;
        }

        public String getStereotype() {
            return stereotype;
        }

        public void setStereotype(String stereotype) {
            this.stereotype = stereotype;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class DataElementPreview {
        private String dataId;
        private String dataName;
        private List<String> evidenceCodes;

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public String getDataName() {
            return dataName;
        }

        public void setDataName(String dataName) {
            this.dataName = dataName;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class FlowPreview {
        private String flowId;
        private String flowType;
        private String sourceType;
        private String sourceId;
        private String sourceName;
        private String targetType;
        private String targetId;
        private String targetName;
        private List<String> dataIds;
        private List<String> dataNames;
        private List<String> evidenceCodes;

        public String getFlowId() {
            return flowId;
        }

        public void setFlowId(String flowId) {
            this.flowId = flowId;
        }

        public String getFlowType() {
            return flowType;
        }

        public void setFlowType(String flowType) {
            this.flowType = flowType;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
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

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
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

        public List<String> getDataIds() {
            return dataIds;
        }

        public void setDataIds(List<String> dataIds) {
            this.dataIds = dataIds;
        }

        public List<String> getDataNames() {
            return dataNames;
        }

        public void setDataNames(List<String> dataNames) {
            this.dataNames = dataNames;
        }

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class ComponentCandidate {
        private String candidateId;
        private String componentType;
        private String name;
        private Integer det;
        private Integer ret;
        private Integer ftr;
        private List<String> evidenceCodes;

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

        public List<String> getEvidenceCodes() {
            return evidenceCodes;
        }

        public void setEvidenceCodes(List<String> evidenceCodes) {
            this.evidenceCodes = evidenceCodes;
        }
    }

    public static class PendingField {
        private String fieldKey;
        private String candidateId;
        private String reason;

        public String getFieldKey() {
            return fieldKey;
        }

        public void setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class EvidenceLine {
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

    public static class ProcessDetails {
        private List<StepCard> cards;
        private List<TableMeta> tables;

        public List<StepCard> getCards() {
            return cards;
        }

        public void setCards(List<StepCard> cards) {
            this.cards = cards;
        }

        public List<TableMeta> getTables() {
            return tables;
        }

        public void setTables(List<TableMeta> tables) {
            this.tables = tables;
        }
    }

    public static class StepCard {
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

    public static class TableMeta {
        private String tableKey;
        private String title;
        private List<ColumnMeta> columns;

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

        public List<ColumnMeta> getColumns() {
            return columns;
        }

        public void setColumns(List<ColumnMeta> columns) {
            this.columns = columns;
        }
    }

    public static class ColumnMeta {
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
