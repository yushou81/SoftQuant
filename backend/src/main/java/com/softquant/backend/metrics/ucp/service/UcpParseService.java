package com.softquant.backend.metrics.ucp.service;

import com.softquant.backend.metrics.shared.contract.MetricContracts;
import com.softquant.backend.metrics.shared.powerdesigner.PowerDesignerXmlSupport;
import com.softquant.backend.metrics.ucp.dto.UcpParseRequest;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
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
public class UcpParseService {

    private static final Map<String, String> UCP_STEREOTYPE_ALIASES = Map.of(
            "INCLUDE", "INCLUDE",
            "EXTEND", "EXTEND"
    );

    private final PowerDesignerXmlSupport xmlSupport;

    public UcpParseService(PowerDesignerXmlSupport xmlSupport) {
        this.xmlSupport = xmlSupport;
    }

    public UcpParseResponse parse(UcpParseRequest request) {
        if (!"POWERDESIGNER_USE_CASE".equalsIgnoreCase(request.sourceType())) {
            throw new ValidationException("sourceType must be POWERDESIGNER_USE_CASE");
        }

        PowerDesignerXmlSupport.ParsedDocument document =
                xmlSupport.parse(request.xmlContent(), "CLD_OBJECT_MODEL");

        Map<String, UcpParseResponse.UcpEvidence> evidenceByCode = new LinkedHashMap<>();
        List<UcpParseResponse.UcpActorPreview> actors = extractActors(document, evidenceByCode);
        Map<String, UseCaseRelationStats> relationStatsByUseCaseId = new LinkedHashMap<>();
        List<UcpParseResponse.UcpRelationshipPreview> relationships =
                extractRelationships(document, evidenceByCode, relationStatsByUseCaseId);
        List<UcpParseResponse.UcpUseCasePreview> useCases =
                extractUseCases(document, relationStatsByUseCaseId, evidenceByCode);

        UcpParseResponse response = new UcpParseResponse();
        response.setModule(MetricContracts.ModuleKey.UCP.name());
        response.setContractKind(MetricContracts.ContractKind.PARSE_PREVIEW.name());
        response.setProjectName(normalizeProjectName(request.projectName(), request.sourceName()));
        response.setSourceName(request.sourceName());
        response.setSourceType(request.sourceType().toUpperCase(Locale.ROOT));
        response.setActors(actors);
        response.setUseCases(useCases);
        response.setRelationships(relationships);
        response.setPendingFields(buildPendingFields(useCases));
        response.setEvidence(new ArrayList<>(evidenceByCode.values()));
        response.setProcessDetails(buildProcessDetails());
        return response;
    }

    private List<UcpParseResponse.UcpActorPreview> extractActors(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, UcpParseResponse.UcpEvidence> evidenceByCode) {
        List<UcpParseResponse.UcpActorPreview> actors = new ArrayList<>();
        for (Element actor : document.descendants("Actor")) {
            if (!actor.hasAttribute("Id")) {
                continue;
            }

            String actorId = actor.getAttribute("Id");
            String actorName = document.requiredText(actor, "Name");
            String stereotype = document.optionalText(actor, "Stereotype");

            UcpParseResponse.UcpActorPreview preview = new UcpParseResponse.UcpActorPreview();
            preview.setActorId(actorId);
            preview.setActorName(actorName);
            preview.setStereotype(stereotype == null ? "" : stereotype);
            preview.setSuggestedComplexity("COMPLEX");
            preview.setSuggestedWeight(3);

            String evidenceCode = "ACTOR_" + actorId;
            preview.setEvidenceCodes(List.of(evidenceCode));
            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_OBJECT.name(),
                    "Actor " + actorName + " 默认按人机交互角色建议为复杂 actor。"
            ));
            actors.add(preview);
        }

        actors.sort(Comparator.comparing(UcpParseResponse.UcpActorPreview::getActorName));
        return actors;
    }

    private List<UcpParseResponse.UcpRelationshipPreview> extractRelationships(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, UcpParseResponse.UcpEvidence> evidenceByCode,
            Map<String, UseCaseRelationStats> relationStatsByUseCaseId) {
        List<UcpParseResponse.UcpRelationshipPreview> relationships = new ArrayList<>();

        // 先抽 actor-usecase 关联，后续前端可以直接画参与者关系图。
        for (Element association : document.descendants("UseCaseAssociation")) {
            if (!association.hasAttribute("Id")) {
                continue;
            }

            Element sourceUseCase = document.refResolver().resolveRequired(
                    document.requireChild(document.requireChild(association, "Object1"), "UseCase"),
                    "UseCase"
            );
            Element targetActor = document.refResolver().resolveRequired(
                    document.requireChild(document.requireChild(association, "Object2"), "Actor"),
                    "Actor"
            );

            String relationshipId = association.getAttribute("Id");
            String sourceId = sourceUseCase.getAttribute("Id");
            String targetId = targetActor.getAttribute("Id");
            String evidenceCode = "REL_" + relationshipId;

            relationships.add(relationship(
                    relationshipId,
                    "ACTOR_ASSOCIATION",
                    sourceId,
                    document.requiredText(sourceUseCase, "Name"),
                    targetId,
                    document.requiredText(targetActor, "Name"),
                    evidenceCode
            ));

            relationStatsByUseCaseId
                    .computeIfAbsent(sourceId, key -> new UseCaseRelationStats())
                    .actorIds()
                    .add(targetId);

            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_RELATION.name(),
                    "UseCaseAssociation " + relationshipId + " 连接用例与参与者。"
            ));
        }

        // include/extend 依赖只保留建议值和证据，不在此阶段直接折算最终复杂度。
        for (Element dependency : document.descendants("Dependency")) {
            if (!dependency.hasAttribute("Id")) {
                continue;
            }

            String stereotype = document.normalizeStereotype(dependency, UCP_STEREOTYPE_ALIASES);
            Element sourceUseCase = document.refResolver().resolveRequired(
                    document.requireChild(document.requireChild(dependency, "Object1"), "UseCase"),
                    "UseCase"
            );
            Element targetUseCase = document.refResolver().resolveRequired(
                    document.requireChild(document.requireChild(dependency, "Object2"), "UseCase"),
                    "UseCase"
            );

            String relationshipId = dependency.getAttribute("Id");
            String sourceId = sourceUseCase.getAttribute("Id");
            String targetId = targetUseCase.getAttribute("Id");
            String evidenceCode = "REL_" + relationshipId;

            relationships.add(relationship(
                    relationshipId,
                    stereotype,
                    sourceId,
                    document.requiredText(sourceUseCase, "Name"),
                    targetId,
                    document.requiredText(targetUseCase, "Name"),
                    evidenceCode
            ));

            UseCaseRelationStats sourceStats = relationStatsByUseCaseId
                    .computeIfAbsent(sourceId, key -> new UseCaseRelationStats());
            sourceStats.relatedUseCaseIds().add(targetId);
            sourceStats.dependencyTypes().add(stereotype);

            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.XML_RELATION.name(),
                    "Dependency " + relationshipId + " 的 stereotype 为 " + stereotype + "。"
            ));
        }

        relationships.sort(Comparator.comparing(UcpParseResponse.UcpRelationshipPreview::getRelationshipId));
        return relationships;
    }

    private List<UcpParseResponse.UcpUseCasePreview> extractUseCases(
            PowerDesignerXmlSupport.ParsedDocument document,
            Map<String, UseCaseRelationStats> relationStatsByUseCaseId,
            Map<String, UcpParseResponse.UcpEvidence> evidenceByCode) {
        List<UcpParseResponse.UcpUseCasePreview> useCases = new ArrayList<>();
        for (Element useCase : document.descendants("UseCase")) {
            if (!useCase.hasAttribute("Id")) {
                continue;
            }

            String useCaseId = useCase.getAttribute("Id");
            String useCaseName = document.requiredText(useCase, "Name");
            UseCaseRelationStats stats = relationStatsByUseCaseId.getOrDefault(useCaseId, new UseCaseRelationStats());

            // PowerDesigner 样例缺少实体数、步骤数、类数，解析阶段只给待补录提示。
            int evidenceCount = Math.max(1, stats.actorIds().size() + stats.relatedUseCaseIds().size());
            String suggestedComplexity = suggestUseCaseComplexity(stats);
            int suggestedWeight = weightForComplexity(suggestedComplexity);
            String evidenceCode = "UC_" + useCaseId;

            UcpParseResponse.UcpUseCasePreview preview = new UcpParseResponse.UcpUseCasePreview();
            preview.setUseCaseId(useCaseId);
            preview.setUseCaseName(useCaseName);
            preview.setEntityCount(null);
            preview.setStepCount(null);
            preview.setClassCount(null);
            preview.setSuggestedComplexity(suggestedComplexity);
            preview.setSuggestedWeight(suggestedWeight);
            preview.setEvidenceCodes(List.of(evidenceCode));
            useCases.add(preview);

            evidenceByCode.put(evidenceCode, evidence(
                    evidenceCode,
                    MetricContracts.EvidenceType.RULE_HINT.name(),
                    "用例 " + useCaseName + " 基于参与者/依赖关系数量给出默认建议，当前可见关系数为 " + evidenceCount + "。"
            ));
        }

        useCases.sort(Comparator.comparing(UcpParseResponse.UcpUseCasePreview::getUseCaseName));
        return useCases;
    }

    private List<UcpParseResponse.UcpPendingField> buildPendingFields(
            List<UcpParseResponse.UcpUseCasePreview> useCases) {
        List<UcpParseResponse.UcpPendingField> pendingFields = new ArrayList<>();
        for (UcpParseResponse.UcpUseCasePreview useCase : useCases) {
            pendingFields.add(pendingField("entityCount", useCase.getUseCaseId(), useCase.getUseCaseName(),
                    "PowerDesigner 用例图未直接携带数据库实体数，需要人工补录。"));
            pendingFields.add(pendingField("stepCount", useCase.getUseCaseId(), useCase.getUseCaseName(),
                    "PowerDesigner 用例图未直接携带步骤数，需要人工补录。"));
            pendingFields.add(pendingField("classCount", useCase.getUseCaseId(), useCase.getUseCaseName(),
                    "PowerDesigner 用例图未直接携带分析类数量，需要人工补录。"));
        }
        return pendingFields;
    }

    private UcpParseResponse.UcpProcessDetails buildProcessDetails() {
        UcpParseResponse.UcpProcessDetails details = new UcpParseResponse.UcpProcessDetails();
        details.setCards(List.of(
                stepCard("load-xml", "加载 XML", "校验 PowerDesigner 用例图类型并建立对象索引。"),
                stepCard("extract-actors", "抽取参与者", "识别 Actor 并给出默认复杂度建议。"),
                stepCard("extract-use-cases", "抽取用例", "识别 UseCase 并整理名称与编号。"),
                stepCard("build-relations", "重建关系图", "合并 actor-usecase、include、extend 三类关系。"),
                stepCard("suggest-complexity", "生成建议值", "根据关系规模给出默认复杂度，并标记待补录字段。")
        ));
        details.setTables(List.of(
                table("actor-classification", "Actor 分类表", List.of(
                        column("actorName", "Actor", "string", true, false),
                        column("stereotype", "Stereotype", "string", false, false),
                        column("suggestedComplexity", "建议复杂度", "enum", true, true),
                        column("suggestedWeight", "建议权重", "integer", true, false)
                )),
                table("use-case-complexity", "UseCase 复杂度表", List.of(
                        column("useCaseName", "UseCase", "string", true, false),
                        column("entityCount", "实体数", "integer", false, true),
                        column("stepCount", "步骤数", "integer", false, true),
                        column("classCount", "类数", "integer", false, true),
                        column("suggestedComplexity", "建议复杂度", "enum", true, true),
                        column("suggestedWeight", "建议权重", "integer", true, false)
                ))
        ));
        return details;
    }

    private String suggestUseCaseComplexity(UseCaseRelationStats stats) {
        int score = stats.actorIds().size() + stats.relatedUseCaseIds().size();
        if (score >= 3) {
            return "COMPLEX";
        }
        if (score >= 2) {
            return "AVERAGE";
        }
        return "SIMPLE";
    }

    private int weightForComplexity(String suggestedComplexity) {
        return switch (suggestedComplexity) {
            case "COMPLEX" -> 15;
            case "AVERAGE" -> 10;
            default -> 5;
        };
    }

    private String normalizeProjectName(String projectName, String sourceName) {
        if (projectName != null && !projectName.isBlank()) {
            return projectName;
        }
        if (sourceName != null && !sourceName.isBlank()) {
            return sourceName;
        }
        return "unnamed-ucp-project";
    }

    private UcpParseResponse.UcpRelationshipPreview relationship(
            String relationshipId,
            String relationshipType,
            String sourceId,
            String sourceName,
            String targetId,
            String targetName,
            String evidenceCode) {
        UcpParseResponse.UcpRelationshipPreview preview = new UcpParseResponse.UcpRelationshipPreview();
        preview.setRelationshipId(relationshipId);
        preview.setRelationshipType(relationshipType);
        preview.setSourceId(sourceId);
        preview.setSourceName(sourceName);
        preview.setTargetId(targetId);
        preview.setTargetName(targetName);
        preview.setEvidenceCodes(List.of(evidenceCode));
        return preview;
    }

    private UcpParseResponse.UcpPendingField pendingField(
            String fieldKey,
            String entityId,
            String entityName,
            String reason) {
        UcpParseResponse.UcpPendingField pendingField = new UcpParseResponse.UcpPendingField();
        pendingField.setFieldKey(fieldKey);
        pendingField.setEntityId(entityId);
        pendingField.setEntityName(entityName);
        pendingField.setReason(reason);
        return pendingField;
    }

    private UcpParseResponse.UcpEvidence evidence(String code, String evidenceType, String summary) {
        UcpParseResponse.UcpEvidence evidence = new UcpParseResponse.UcpEvidence();
        evidence.setCode(code);
        evidence.setEvidenceType(evidenceType);
        evidence.setSummary(summary);
        return evidence;
    }

    private UcpParseResponse.UcpStepCard stepCard(String stepKey, String title, String description) {
        UcpParseResponse.UcpStepCard card = new UcpParseResponse.UcpStepCard();
        card.setStepKey(stepKey);
        card.setTitle(title);
        card.setDescription(description);
        return card;
    }

    private UcpParseResponse.UcpTableMeta table(
            String tableKey,
            String title,
            List<UcpParseResponse.UcpColumnMeta> columns) {
        UcpParseResponse.UcpTableMeta table = new UcpParseResponse.UcpTableMeta();
        table.setTableKey(tableKey);
        table.setTitle(title);
        table.setColumns(columns);
        return table;
    }

    private UcpParseResponse.UcpColumnMeta column(
            String key,
            String label,
            String valueType,
            boolean required,
            boolean editable) {
        UcpParseResponse.UcpColumnMeta column = new UcpParseResponse.UcpColumnMeta();
        column.setKey(key);
        column.setLabel(label);
        column.setValueType(valueType);
        column.setRequired(required);
        column.setEditable(editable);
        return column;
    }

    private record UseCaseRelationStats(
            Set<String> actorIds,
            Set<String> relatedUseCaseIds,
            Set<String> dependencyTypes
    ) {
        private UseCaseRelationStats() {
            this(new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }
}
