package com.softquant.backend.metrics.fp.service;

import com.softquant.backend.metrics.fp.dto.FpParseRequest;
import com.softquant.backend.metrics.fp.dto.FpParseResponse;
import com.softquant.backend.metrics.shared.contract.MetricContracts;
import com.softquant.backend.metrics.shared.powerdesigner.PowerDesignerXmlSupport;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

@Service
public class FpParseService {

    private static final Map<String, String> DFD_STEREOTYPE_ALIASES = Map.of(
            "EXTERNAL_ENTITY", "EXTERNAL_ENTITY",
            "DATA_STORE", "DATA_STORE"
    );

    private final PowerDesignerXmlSupport xmlSupport;

    public FpParseService(PowerDesignerXmlSupport xmlSupport) {
        this.xmlSupport = xmlSupport;
    }

    public FpParseResponse parse(FpParseRequest request) {
        if (!"POWERDESIGNER_DFD".equalsIgnoreCase(request.sourceType())) {
            throw new ValidationException("sourceType must be POWERDESIGNER_DFD");
        }

        PowerDesignerXmlSupport.ParsedDocument document =
                xmlSupport.parse(request.xmlContent(), "BPM_MODEL_XML");

        Map<String, FpParseResponse.EvidenceLine> evidenceByCode = new LinkedHashMap<>();
        Map<String, ProcessNode> processMap = extractProcesses(document, evidenceByCode);
        Map<String, ExternalEntityNode> externalEntityMap = extractExternalEntities(document, evidenceByCode);
        Map<String, DataStoreNode> dataStoreMap = extractDataStores(document, evidenceByCode);
        Map<String, DataNode> dataMap = extractDataElements(document, evidenceByCode);
        List<FpParseResponse.FlowPreview> flows = extractFlows(
                document, processMap, externalEntityMap, dataStoreMap, dataMap, evidenceByCode
        );
        List<FpParseResponse.ComponentCandidate> componentCandidates =
                inferCandidates(flows, processMap, externalEntityMap, dataStoreMap, dataMap, evidenceByCode);

        FpParseResponse response = new FpParseResponse();
        response.setModule(MetricContracts.ModuleKey.FP.name());
        response.setContractKind(MetricContracts.ContractKind.PARSE_PREVIEW.name());
        response.setProjectName(normalizeProjectName(request.projectName(), request.sourceName()));
        response.setSourceName(request.sourceName());
        response.setSourceType(request.sourceType().toUpperCase(Locale.ROOT));
        response.setProcesses(processMap.values().stream().map(ProcessNode::preview).toList());
        response.setExternalEntities(externalEntityMap.values().stream().map(ExternalEntityNode::preview).toList());
        response.setDataStores(dataStoreMap.values().stream().map(DataStoreNode::preview).toList());
        response.setDataElements(dataMap.values().stream().map(DataNode::preview).toList());
        response.setFlows(flows);
        response.setComponentCandidates(componentCandidates);
        response.setPendingFields(buildPendingFields(componentCandidates));
        response.setEvidence(new ArrayList<>(evidenceByCode.values()));
        response.setProcessDetails(buildProcessDetails());
        return response;
    }

    private Map<String, ProcessNode> extractProcesses(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        Map<String, ProcessNode> processMap = new LinkedHashMap<>();
        for (Element process : document.descendants("Process")) {
            if (!process.hasAttribute("Id")) {
                continue;
            }

            String processId = process.getAttribute("Id");
            String processName = document.requiredText(process, "Name");
            String numberId = document.optionalText(process, "NumberID");
            String evidenceCode = "PROCESS_" + processId;

            processMap.put(processId, new ProcessNode(processId, processName, numberId, List.of(evidenceCode)));
            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_OBJECT.name(),
                    "识别 Process 节点 " + processName + "。"
            ));
        }
        return processMap;
    }

    private Map<String, ExternalEntityNode> extractExternalEntities(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        Map<String, ExternalEntityNode> entityMap = new LinkedHashMap<>();
        for (Element entity : document.descendants("OrganizationUnit")) {
            if (!entity.hasAttribute("Id")) {
                continue;
            }

            String normalized = document.normalizeStereotype(entity, DFD_STEREOTYPE_ALIASES);
            if (!"EXTERNAL_ENTITY".equals(normalized)) {
                continue;
            }

            String entityId = entity.getAttribute("Id");
            String entityName = document.requiredText(entity, "Name");
            String evidenceCode = "ENTITY_" + entityId;

            entityMap.put(entityId, new ExternalEntityNode(entityId, entityName, normalized, List.of(evidenceCode)));
            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_OBJECT.name(),
                    "识别 External Entity 节点 " + entityName + "。"
            ));
        }
        return entityMap;
    }

    private Map<String, DataStoreNode> extractDataStores(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        Map<String, DataStoreNode> dataStoreMap = new LinkedHashMap<>();
        for (Element store : document.descendants("Resource")) {
            if (!store.hasAttribute("Id")) {
                continue;
            }

            String normalized = document.normalizeStereotype(store, DFD_STEREOTYPE_ALIASES);
            if (!"DATA_STORE".equals(normalized)) {
                continue;
            }

            String storeId = store.getAttribute("Id");
            String storeName = document.requiredText(store, "Name");
            String evidenceCode = "STORE_" + storeId;

            dataStoreMap.put(storeId, new DataStoreNode(storeId, storeName, normalized, List.of(evidenceCode)));
            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_OBJECT.name(),
                    "识别 Data Store 节点 " + storeName + "。"
            ));
        }
        return dataStoreMap;
    }

    private Map<String, DataNode> extractDataElements(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        Map<String, DataNode> dataMap = new LinkedHashMap<>();
        for (Element data : document.descendants("Data")) {
            if (!data.hasAttribute("Id")) {
                continue;
            }

            String dataId = data.getAttribute("Id");
            String dataName = document.requiredText(data, "Name");
            String evidenceCode = "DATA_" + dataId;

            dataMap.put(dataId, new DataNode(dataId, dataName, List.of(evidenceCode)));
            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_OBJECT.name(),
                    "识别 Data 节点 " + dataName + "。"
            ));
        }
        return dataMap;
    }

    private List<FpParseResponse.FlowPreview> extractFlows(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap,
            Map<String, DataStoreNode> dataStoreMap,
            Map<String, DataNode> dataMap,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        List<FpParseResponse.FlowPreview> flows = new ArrayList<>();
        Map<String, FlowEndpoints> flowSymbolEndpoints =
                extractFlowSymbolEndpoints(document, processMap, externalEntityMap);

        for (Element flow : document.descendants("Flow")) {
            if (!flow.hasAttribute("Id")) {
                continue;
            }
            flows.add(buildFlowPreview(
                    document,
                    flow,
                    "FLOW",
                    flowSymbolEndpoints,
                    processMap,
                    externalEntityMap,
                    dataStoreMap,
                    dataMap,
                    evidenceByCode
            ));
        }

        for (Element flow : document.descendants("ResourceFlow")) {
            if (!flow.hasAttribute("Id")) {
                continue;
            }
            flows.add(buildFlowPreview(
                    document,
                    flow,
                    "RESOURCE_FLOW",
                    flowSymbolEndpoints,
                    processMap,
                    externalEntityMap,
                    dataStoreMap,
                    dataMap,
                    evidenceByCode
            ));
        }

        flows.sort(Comparator.comparing(FpParseResponse.FlowPreview::getFlowId));
        return flows;
    }

    private FpParseResponse.FlowPreview buildFlowPreview(
            PowerDesignerXmlSupport.ParsedDocument document,
            Element flow,
            String flowType,
            Map<String, FlowEndpoints> flowSymbolEndpoints,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap,
            Map<String, DataStoreNode> dataStoreMap,
            Map<String, DataNode> dataMap,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        FlowEndpoint source;
        FlowEndpoint target;

        if ("FLOW".equals(flowType)) {
            FlowEndpoints endpoints = flowSymbolEndpoints.get(flow.getAttribute("Id"));
            if (endpoints == null) {
                source = resolveGeneralEndpoint(document, document.requireChild(flow, "Object1"), processMap, externalEntityMap);
                target = resolveGeneralEndpoint(document, document.requireChild(flow, "Object2"), processMap, externalEntityMap);
            } else {
                source = endpoints.source();
                target = endpoints.target();
            }
        } else {
            source = resolveResourceFlowSource(document, flow, processMap, dataStoreMap);
            target = resolveResourceFlowTarget(document, flow, processMap, dataStoreMap);
        }

        List<String> dataIds = new ArrayList<>();
        List<String> dataNames = new ArrayList<>();
        for (Element dataRef : document.children(document.requireChild(flow, "MessageFlow.Data"), "Data")) {
            Element data = document.refResolver().resolveRequired(dataRef, "Data");
            String dataId = data.getAttribute("Id");
            dataIds.add(dataId);
            dataNames.add(dataMap.get(dataId).dataName());
        }

        String flowId = flow.getAttribute("Id");
        String evidenceCode = "FLOW_" + flowId;
        evidenceByCode.put(evidenceCode, evidence(
                evidenceCode,
                MetricContracts.EvidenceType.XML_RELATION.name(),
                "识别 " + flowType + " " + flowId + "，连接 " + source.name() + " -> " + target.name() + "。"
        ));

        FpParseResponse.FlowPreview preview = new FpParseResponse.FlowPreview();
        preview.setFlowId(flowId);
        preview.setFlowType(flowType);
        preview.setSourceType(source.type());
        preview.setSourceId(source.id());
        preview.setSourceName(source.name());
        preview.setTargetType(target.type());
        preview.setTargetId(target.id());
        preview.setTargetName(target.name());
        preview.setDataIds(dataIds);
        preview.setDataNames(dataNames);
        preview.setEvidenceCodes(List.of(evidenceCode));
        return preview;
    }

    private List<FpParseResponse.ComponentCandidate> inferCandidates(
            List<FpParseResponse.FlowPreview> flows,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap,
            Map<String, DataStoreNode> dataStoreMap,
            Map<String, DataNode> dataMap,
            Map<String, FpParseResponse.EvidenceLine> evidenceByCode) {
        Map<String, FpParseResponse.ComponentCandidate> candidates = new LinkedHashMap<>();

        // Data Store 先固化为 ILF 候选；若后面接入跨系统数据源，再补 EIF 判定。
        for (DataStoreNode store : dataStoreMap.values()) {
            String candidateId = "CAND_" + store.storeId();
            candidates.put(candidateId, candidate(candidateId, "ILF", store.storeName(), 1, 1, null, store.evidenceCodes()));
            evidenceByCode.put("CAND_" + candidateId, evidence(
                    "CAND_" + candidateId,
                    MetricContracts.EvidenceType.RULE_HINT.name(),
                    "Data Store " + store.storeName() + " 默认生成 ILF 候选。"
            ));
        }

        // 外部实体到处理过程的输入流，先推 EI 候选。
        for (FpParseResponse.FlowPreview flow : flows) {
            if ("EXTERNAL_ENTITY".equals(flow.getSourceType()) && "PROCESS".equals(flow.getTargetType())) {
                String candidateId = "CAND_EI_" + flow.getFlowId();
                candidates.put(candidateId, candidate(
                        candidateId,
                        "EI",
                        flow.getTargetName() + " 输入",
                        flow.getDataIds().size(),
                        null,
                        countReferencedStoresForProcess(flow.getTargetId(), flows),
                        flow.getEvidenceCodes()
                ));
            }

            if ("PROCESS".equals(flow.getSourceType()) && "EXTERNAL_ENTITY".equals(flow.getTargetType())) {
                String candidateId = "CAND_EO_" + flow.getFlowId();
                candidates.put(candidateId, candidate(
                        candidateId,
                        "EO",
                        flow.getSourceName() + " 输出",
                        flow.getDataIds().size(),
                        null,
                        countReferencedStoresForProcess(flow.getSourceId(), flows),
                        flow.getEvidenceCodes()
                ));
            }

            // 过程到过程的纯查询风格流暂记为 EQ 候选，方便前端二阶段确认。
            if ("PROCESS".equals(flow.getSourceType()) && "PROCESS".equals(flow.getTargetType())) {
                String candidateId = "CAND_EQ_" + flow.getFlowId();
                candidates.put(candidateId, candidate(
                        candidateId,
                        "EQ",
                        flow.getTargetName() + " 查询",
                        flow.getDataIds().size(),
                        null,
                        countReferencedStoresForProcessPair(flow.getSourceId(), flow.getTargetId(), flows),
                        flow.getEvidenceCodes()
                ));
            }

            if ("RESOURCE_FLOW".equals(flow.getFlowType())
                    && "DATA_STORE".equals(flow.getSourceType())
                    && "PROCESS".equals(flow.getTargetType())) {
                String candidateId = "CAND_EO_" + flow.getFlowId();
                candidates.put(candidateId, candidate(
                        candidateId,
                        "EO",
                        flow.getTargetName() + " 输出",
                        flow.getDataIds().size(),
                        null,
                        countReferencedStoresForProcess(flow.getTargetId(), flows),
                        flow.getEvidenceCodes()
                ));
            }
        }

        List<FpParseResponse.ComponentCandidate> sorted = new ArrayList<>(candidates.values());
        sorted.sort(Comparator.comparing(FpParseResponse.ComponentCandidate::getCandidateId));
        return sorted;
    }

    private List<FpParseResponse.PendingField> buildPendingFields(List<FpParseResponse.ComponentCandidate> candidates) {
        List<FpParseResponse.PendingField> pendingFields = new ArrayList<>();
        for (FpParseResponse.ComponentCandidate candidate : candidates) {
            if (candidate.getDet() == null) {
                pendingFields.add(pendingField("det", candidate.getCandidateId(), "DET 需要人工确认。"));
            }
            if (candidate.getComponentType().equals("ILF") || candidate.getComponentType().equals("EIF")) {
                pendingFields.add(pendingField("ret", candidate.getCandidateId(), "RET 需要人工确认。"));
            } else {
                pendingFields.add(pendingField("ftr", candidate.getCandidateId(), "FTR 需要人工确认。"));
            }
        }
        return pendingFields;
    }

    private FpParseResponse.ProcessDetails buildProcessDetails() {
        FpParseResponse.ProcessDetails details = new FpParseResponse.ProcessDetails();
        details.setCards(List.of(
                stepCard("load-xml", "加载 XML", "校验 DFD 类型并建立对象索引。"),
                stepCard("extract-dfd-objects", "抽取 DFD 对象", "识别 Process、External Entity、Data Store、Data。"),
                stepCard("resolve-flows", "重建流向", "抽取 Flow / ResourceFlow 并补齐数据项。"),
                stepCard("generate-candidates", "生成候选", "按流向模式生成 ILF/EI/EO/EQ 候选。"),
                stepCard("mark-pending-fields", "标记待确认项", "为 DET/RET/FTR 草稿补齐人工确认入口。")
        ));
        details.setTables(List.of(
                table("function-component-candidates", "功能组件候选表", List.of(
                        column("componentType", "组件类型", "enum", true, true),
                        column("name", "候选名称", "string", true, false),
                        column("det", "DET", "integer", false, true),
                        column("ret", "RET", "integer", false, true),
                        column("ftr", "FTR", "integer", false, true),
                        column("evidenceCodes", "证据", "string[]", true, false)
                )),
                table("flow-evidence", "流向证据表", List.of(
                        column("flowId", "流编号", "string", true, false),
                        column("sourceName", "源节点", "string", true, false),
                        column("targetName", "目标节点", "string", true, false),
                        column("dataNames", "数据项", "string[]", true, false)
                ))
        ));
        return details;
    }

    private Map<String, FlowEndpoints> extractFlowSymbolEndpoints(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap) {
        Map<String, FlowEndpoints> endpointsByFlowId = new LinkedHashMap<>();
        for (Element symbol : document.descendants("FlowSymbol")) {
            Element objectHolder = document.requireChild(symbol, "Object");
            List<Element> flowRefs = document.children(objectHolder, "Flow");
            if (flowRefs.isEmpty()) {
                continue;
            }

            Element flow = document.refResolver().resolveRequired(flowRefs.get(0), "Flow");
            FlowEndpoint source = resolveFlowSymbolEndpoint(
                    document,
                    document.requireChild(symbol, "SourceSymbol"),
                    processMap,
                    externalEntityMap
            );
            FlowEndpoint target = resolveFlowSymbolEndpoint(
                    document,
                    document.requireChild(symbol, "DestinationSymbol"),
                    processMap,
                    externalEntityMap
            );
            endpointsByFlowId.put(flow.getAttribute("Id"), new FlowEndpoints(source, target));
        }
        return endpointsByFlowId;
    }

    private FlowEndpoint resolveFlowSymbolEndpoint(
            PowerDesignerXmlSupport.ParsedDocument document,
            Element symbolHolder,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap) {
        List<Element> processSymbols = document.children(symbolHolder, "ProcessSymbol");
        if (!processSymbols.isEmpty()) {
            Element symbol = document.refResolver().resolveRequired(processSymbols.get(0), "ProcessSymbol");
            return resolveGeneralEndpoint(document, document.requireChild(symbol, "Object"), processMap, externalEntityMap);
        }

        List<Element> entitySymbols = document.children(symbolHolder, "OrganizationUnitSymbol");
        if (!entitySymbols.isEmpty()) {
            Element symbol = document.refResolver().resolveRequired(entitySymbols.get(0), "OrganizationUnitSymbol");
            return resolveGeneralEndpoint(document, document.requireChild(symbol, "Object"), processMap, externalEntityMap);
        }

        throw new ValidationException("Unsupported DFD flow symbol endpoint under " + symbolHolder.getLocalName());
    }

    private FlowEndpoint resolveGeneralEndpoint(
            PowerDesignerXmlSupport.ParsedDocument document,
            Element holder,
            Map<String, ProcessNode> processMap,
            Map<String, ExternalEntityNode> externalEntityMap) {
        List<Element> processes = document.children(holder, "Process");
        if (!processes.isEmpty()) {
            Element process = document.refResolver().resolveRequired(processes.get(0), "Process");
            ProcessNode node = processMap.get(process.getAttribute("Id"));
            return new FlowEndpoint("PROCESS", node.processId(), node.processName());
        }

        List<Element> entities = document.children(holder, "OrganizationUnit");
        if (!entities.isEmpty()) {
            Element entity = document.refResolver().resolveRequired(entities.get(0), "OrganizationUnit");
            ExternalEntityNode node = externalEntityMap.get(entity.getAttribute("Id"));
            return new FlowEndpoint("EXTERNAL_ENTITY", node.entityId(), node.entityName());
        }

        throw new ValidationException("Unsupported DFD flow endpoint under " + holder.getLocalName());
    }

    private FlowEndpoint resolveResourceFlowSource(
            PowerDesignerXmlSupport.ParsedDocument document,
            Element flow,
            Map<String, ProcessNode> processMap,
            Map<String, DataStoreNode> dataStoreMap) {
        Element process = document.refResolver().resolveRequired(
                document.requireChild(document.requireChild(flow, "Process"), "Process"),
                "Process"
        );
        ProcessNode node = processMap.get(process.getAttribute("Id"));

        String access = document.optionalText(flow, "ReadAccess");
        if ("1".equals(access)) {
            Element store = document.refResolver().resolveRequired(
                    document.requireChild(document.requireChild(flow, "Resource"), "Resource"),
                    "Resource"
            );
            DataStoreNode storeNode = dataStoreMap.get(store.getAttribute("Id"));
            return new FlowEndpoint("DATA_STORE", storeNode.storeId(), storeNode.storeName());
        }
        return new FlowEndpoint("PROCESS", node.processId(), node.processName());
    }

    private FlowEndpoint resolveResourceFlowTarget(
            PowerDesignerXmlSupport.ParsedDocument document,
            Element flow,
            Map<String, ProcessNode> processMap,
            Map<String, DataStoreNode> dataStoreMap) {
        Element process = document.refResolver().resolveRequired(
                document.requireChild(document.requireChild(flow, "Process"), "Process"),
                "Process"
        );
        ProcessNode node = processMap.get(process.getAttribute("Id"));

        String access = document.optionalText(flow, "ReadAccess");
        if ("1".equals(access)) {
            return new FlowEndpoint("PROCESS", node.processId(), node.processName());
        }

        Element store = document.refResolver().resolveRequired(
                document.requireChild(document.requireChild(flow, "Resource"), "Resource"),
                "Resource"
        );
        DataStoreNode storeNode = dataStoreMap.get(store.getAttribute("Id"));
        return new FlowEndpoint("DATA_STORE", storeNode.storeId(), storeNode.storeName());
    }

    private Integer countReferencedStoresForProcess(String processId, List<FpParseResponse.FlowPreview> flows) {
        Set<String> stores = new LinkedHashSet<>();
        for (FpParseResponse.FlowPreview flow : flows) {
            if (processId.equals(flow.getSourceId()) && "DATA_STORE".equals(flow.getTargetType())) {
                stores.add(flow.getTargetId());
            }
            if (processId.equals(flow.getTargetId()) && "DATA_STORE".equals(flow.getSourceType())) {
                stores.add(flow.getSourceId());
            }
        }
        return stores.size();
    }

    private Integer countReferencedStoresForProcessPair(
            String firstProcessId,
            String secondProcessId,
            List<FpParseResponse.FlowPreview> flows) {
        Set<String> stores = new LinkedHashSet<>();
        collectReferencedStoresForProcess(firstProcessId, flows, stores);
        collectReferencedStoresForProcess(secondProcessId, flows, stores);
        return stores.size();
    }

    private void collectReferencedStoresForProcess(
            String processId,
            List<FpParseResponse.FlowPreview> flows,
            Set<String> stores) {
        for (FpParseResponse.FlowPreview flow : flows) {
            if (processId.equals(flow.getSourceId()) && "DATA_STORE".equals(flow.getTargetType())) {
                stores.add(flow.getTargetId());
            }
            if (processId.equals(flow.getTargetId()) && "DATA_STORE".equals(flow.getSourceType())) {
                stores.add(flow.getSourceId());
            }
        }
    }

    private FpParseResponse.ComponentCandidate candidate(
            String candidateId,
            String componentType,
            String name,
            Integer det,
            Integer ret,
            Integer ftr,
            List<String> evidenceCodes) {
        FpParseResponse.ComponentCandidate candidate = new FpParseResponse.ComponentCandidate();
        candidate.setCandidateId(candidateId);
        candidate.setComponentType(componentType);
        candidate.setName(name);
        candidate.setDet(det);
        candidate.setRet(ret);
        candidate.setFtr(ftr);
        candidate.setEvidenceCodes(evidenceCodes);
        return candidate;
    }

    private FpParseResponse.PendingField pendingField(String fieldKey, String candidateId, String reason) {
        FpParseResponse.PendingField pendingField = new FpParseResponse.PendingField();
        pendingField.setFieldKey(fieldKey);
        pendingField.setCandidateId(candidateId);
        pendingField.setReason(reason);
        return pendingField;
    }

    private FpParseResponse.EvidenceLine evidence(String code, String evidenceType, String summary) {
        FpParseResponse.EvidenceLine evidence = new FpParseResponse.EvidenceLine();
        evidence.setCode(code);
        evidence.setEvidenceType(evidenceType);
        evidence.setSummary(summary);
        return evidence;
    }

    private FpParseResponse.StepCard stepCard(String stepKey, String title, String description) {
        FpParseResponse.StepCard card = new FpParseResponse.StepCard();
        card.setStepKey(stepKey);
        card.setTitle(title);
        card.setDescription(description);
        return card;
    }

    private FpParseResponse.TableMeta table(String tableKey, String title, List<FpParseResponse.ColumnMeta> columns) {
        FpParseResponse.TableMeta table = new FpParseResponse.TableMeta();
        table.setTableKey(tableKey);
        table.setTitle(title);
        table.setColumns(columns);
        return table;
    }

    private FpParseResponse.ColumnMeta column(
            String key,
            String label,
            String valueType,
            boolean required,
            boolean editable) {
        FpParseResponse.ColumnMeta column = new FpParseResponse.ColumnMeta();
        column.setKey(key);
        column.setLabel(label);
        column.setValueType(valueType);
        column.setRequired(required);
        column.setEditable(editable);
        return column;
    }

    private String normalizeProjectName(String projectName, String sourceName) {
        if (projectName != null && !projectName.isBlank()) {
            return projectName;
        }
        if (sourceName != null && !sourceName.isBlank()) {
            return sourceName;
        }
        return "unnamed-fp-project";
    }

    private record ProcessNode(String processId, String processName, String numberId, List<String> evidenceCodes) {
        private FpParseResponse.DfdProcessPreview preview() {
            FpParseResponse.DfdProcessPreview preview = new FpParseResponse.DfdProcessPreview();
            preview.setProcessId(processId);
            preview.setProcessName(processName);
            preview.setNumberId(numberId);
            preview.setEvidenceCodes(evidenceCodes);
            return preview;
        }
    }

    private record ExternalEntityNode(String entityId, String entityName, String stereotype,
                                      List<String> evidenceCodes) {
        private FpParseResponse.ExternalEntityPreview preview() {
            FpParseResponse.ExternalEntityPreview preview = new FpParseResponse.ExternalEntityPreview();
            preview.setEntityId(entityId);
            preview.setEntityName(entityName);
            preview.setStereotype(stereotype);
            preview.setEvidenceCodes(evidenceCodes);
            return preview;
        }
    }

    private record DataStoreNode(String storeId, String storeName, String stereotype,
                                 List<String> evidenceCodes) {
        private FpParseResponse.DataStorePreview preview() {
            FpParseResponse.DataStorePreview preview = new FpParseResponse.DataStorePreview();
            preview.setStoreId(storeId);
            preview.setStoreName(storeName);
            preview.setStereotype(stereotype);
            preview.setEvidenceCodes(evidenceCodes);
            return preview;
        }
    }

    private record DataNode(String dataId, String dataName, List<String> evidenceCodes) {
        private FpParseResponse.DataElementPreview preview() {
            FpParseResponse.DataElementPreview preview = new FpParseResponse.DataElementPreview();
            preview.setDataId(dataId);
            preview.setDataName(dataName);
            preview.setEvidenceCodes(evidenceCodes);
            return preview;
        }
    }

    private record FlowEndpoint(String type, String id, String name) {
    }

    private record FlowEndpoints(FlowEndpoint source, FlowEndpoint target) {
    }
}
