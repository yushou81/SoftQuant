package com.softquant.backend.metrics.shared.contract;

import java.math.BigDecimal;
import java.util.List;

public final class MetricContracts {

    public static final BigDecimal DEFAULT_UCP_PRODUCTIVITY = bd("28");
    public static final String DEFAULT_UCP_PRODUCTIVITY_UNIT = "person-hours-per-ucp";
    public static final int DEFAULT_JAVA_SLOC_PER_FP = 60;

    public static final List<WeightedOption> UCP_ACTOR_COMPLEXITIES = List.of(
            new WeightedOption("ACTOR_SIMPLE", "Simple actor", ComplexityLevel.SIMPLE, 1,
                    "Another system interacting through a defined API."),
            new WeightedOption("ACTOR_AVERAGE", "Average actor", ComplexityLevel.AVERAGE, 2,
                    "Another system interacting through a protocol such as TCP/IP."),
            new WeightedOption("ACTOR_COMPLEX", "Complex actor", ComplexityLevel.COMPLEX, 3,
                    "A human actor interacting through a user interface.")
    );

    public static final List<UseCaseRule> UCP_USE_CASE_RULES = List.of(
            new UseCaseRule("ENTITY_COUNT", 1, 1, ComplexityLevel.SIMPLE, 5),
            new UseCaseRule("ENTITY_COUNT", 2, 2, ComplexityLevel.AVERAGE, 10),
            new UseCaseRule("ENTITY_COUNT", 3, null, ComplexityLevel.COMPLEX, 15),
            new UseCaseRule("STEP_COUNT", 0, 3, ComplexityLevel.SIMPLE, 5),
            new UseCaseRule("STEP_COUNT", 4, 7, ComplexityLevel.AVERAGE, 10),
            new UseCaseRule("STEP_COUNT", 8, null, ComplexityLevel.COMPLEX, 15),
            new UseCaseRule("CLASS_COUNT", 0, 4, ComplexityLevel.SIMPLE, 5),
            new UseCaseRule("CLASS_COUNT", 5, 10, ComplexityLevel.AVERAGE, 10),
            new UseCaseRule("CLASS_COUNT", 11, null, ComplexityLevel.COMPLEX, 15)
    );

    public static final List<FactorDefinition> UCP_TECHNICAL_FACTORS = List.of(
            new FactorDefinition("T1", "Distributed system", bd("2"), "PPT slide 49"),
            new FactorDefinition("T2", "Response or throughput performance", bd("1"), "PPT slide 49"),
            new FactorDefinition("T3", "End-user efficiency", bd("1"), "PPT slide 49"),
            new FactorDefinition("T4", "Complex internal processing", bd("1"), "PPT slide 49"),
            new FactorDefinition("T5", "Reusability", bd("1"), "PPT slide 49"),
            new FactorDefinition("T6", "Easy to install", bd("0.5"), "PPT slide 49"),
            new FactorDefinition("T7", "Easy to use", bd("0.5"), "PPT slide 49"),
            new FactorDefinition("T8", "Portability", bd("2"), "PPT slide 49"),
            new FactorDefinition("T9", "Easy to change", bd("1"), "PPT slide 49"),
            new FactorDefinition("T10", "Concurrency", bd("1"), "PPT slide 49"),
            new FactorDefinition("T11", "Special security", bd("1"), "PPT slide 49"),
            new FactorDefinition("T12", "Provide third-party interfaces", bd("1"), "PPT slide 49"),
            new FactorDefinition("T13", "Special user training", bd("1"), "PPT slide 49")
    );

    public static final List<FactorDefinition> UCP_ENVIRONMENTAL_FACTORS = List.of(
            new FactorDefinition("E1", "Familiar with UML", bd("1.5"), "PPT slide 54"),
            new FactorDefinition("E2", "Application experience", bd("0.5"), "PPT slide 54"),
            new FactorDefinition("E3", "Object-oriented experience", bd("1"), "PPT slide 54"),
            new FactorDefinition("E4", "Lead analyst capability", bd("0.5"), "PPT slide 54"),
            new FactorDefinition("E5", "Motivation", bd("1"), "PPT slide 54"),
            new FactorDefinition("E6", "Stable requirements", bd("2"), "PPT slide 54"),
            new FactorDefinition("E7", "Part-time staff", bd("-1"), "PPT slide 54"),
            new FactorDefinition("E8", "Difficult programming language", bd("-1"), "PPT slide 54")
    );

    public static final List<FormulaDefinition> UCP_FORMULAS = List.of(
            new FormulaDefinition("UAW", "UAW = sum(actorWeight)", "uaw",
                    "Unadjusted actor weight from actor complexity buckets."),
            new FormulaDefinition("UUC", "UUC = sum(useCaseWeight)", "uuc",
                    "Unadjusted use case weight from entity, step, and class heuristics."),
            new FormulaDefinition("UUCP", "UUCP = UAW + UUC", "uucp", "PPT slide 41"),
            new FormulaDefinition("TCF", "TCF = 0.6 + 0.01 * TFactor", "tcf", "PPT slide 41"),
            new FormulaDefinition("EF", "EF = 1.4 - 0.03 * EFactor", "ef", "PPT slide 41"),
            new FormulaDefinition("UCP", "UCP = UUCP * TCF * EF", "ucp", "PPT slide 41"),
            new FormulaDefinition("EFFORT", "Effort = UCP * productivity", "effort", "PPT slide 55")
    );

    public static final List<FunctionComponentWeight> FP_COMPONENT_WEIGHTS = List.of(
            new FunctionComponentWeight(FunctionComponentType.ILF, ComplexityLevel.SIMPLE, 7, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.ILF, ComplexityLevel.AVERAGE, 10, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.ILF, ComplexityLevel.COMPLEX, 15, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.EIF, ComplexityLevel.SIMPLE, 5, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.EIF, ComplexityLevel.AVERAGE, 7, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.EIF, ComplexityLevel.COMPLEX, 10, "DET/RET"),
            new FunctionComponentWeight(FunctionComponentType.EI, ComplexityLevel.SIMPLE, 3, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EI, ComplexityLevel.AVERAGE, 4, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EI, ComplexityLevel.COMPLEX, 6, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EO, ComplexityLevel.SIMPLE, 4, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EO, ComplexityLevel.AVERAGE, 5, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EO, ComplexityLevel.COMPLEX, 7, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EQ, ComplexityLevel.SIMPLE, 3, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EQ, ComplexityLevel.AVERAGE, 4, "DET/FTR"),
            new FunctionComponentWeight(FunctionComponentType.EQ, ComplexityLevel.COMPLEX, 6, "DET/FTR")
    );

    public static final List<GscDefinition> FP_GENERAL_SYSTEM_CHARACTERISTICS = List.of(
            new GscDefinition("G1", "Data communications", "PPT slide 21"),
            new GscDefinition("G2", "Distributed data processing", "PPT slide 21"),
            new GscDefinition("G3", "Performance", "PPT slide 21"),
            new GscDefinition("G4", "Heavily used configuration", "PPT slide 21"),
            new GscDefinition("G5", "Transaction rate", "PPT slide 21"),
            new GscDefinition("G6", "Online data entry", "PPT slide 21"),
            new GscDefinition("G7", "End-user efficiency", "PPT slide 21"),
            new GscDefinition("G8", "Online update", "PPT slide 21"),
            new GscDefinition("G9", "Complex processing", "PPT slide 21"),
            new GscDefinition("G10", "Reusability", "PPT slide 21"),
            new GscDefinition("G11", "Installation ease", "PPT slide 21"),
            new GscDefinition("G12", "Operational ease", "PPT slide 21"),
            new GscDefinition("G13", "Multiple sites", "PPT slide 21"),
            new GscDefinition("G14", "Facilitate change", "PPT slide 21")
    );

    public static final List<LanguageProfile> FP_LANGUAGE_PROFILES = List.of(
            new LanguageProfile("JAVA", "Java", DEFAULT_JAVA_SLOC_PER_FP, true)
    );

    public static final List<FormulaDefinition> FP_FORMULAS = List.of(
            new FormulaDefinition("UFP", "UFP = sum(componentWeight)", "ufp", "PPT slide 20"),
            new FormulaDefinition("VAF", "VAF = 0.65 + 0.01 * sum(gscScore)", "vaf", "PPT slide 22"),
            new FormulaDefinition("FP", "FP = UFP * VAF", "fp", "PPT slide 22"),
            new FormulaDefinition("LOC", "LOC = FP * slocPerFp", "locEstimate",
                    "Project default keeps Java at 60 SLOC/FP.")
    );

    public static final EndpointContract UCP_PARSE_PREVIEW_CONTRACT = new EndpointContract(
            ModuleKey.UCP,
            ContractKind.PARSE_PREVIEW,
            "POST /api/metrics/ucp/parse",
            SourceModelType.POWERDESIGNER_USE_CASE,
            "Parse PowerDesigner use-case XML and emit preview data plus editable hints.",
            List.of(
                    field("projectName", "string", true, "Display name for the current analysis."),
                    field("sourceName", "string", true, "Original XML file name."),
                    field("sourceType", "enum", true, "Must be POWERDESIGNER_USE_CASE."),
                    field("xmlContent", "string", true, "Raw PowerDesigner XML content.")
            ),
            List.of(
                    field("module", "enum", true, "Always UCP."),
                    field("contractKind", "enum", true, "Always PARSE_PREVIEW."),
                    field("actors[].actorId", "string", true, "Stable actor identifier."),
                    field("actors[].actorName", "string", true, "Actor name from XML."),
                    field("actors[].stereotype", "string", false, "Normalized stereotype label."),
                    field("actors[].suggestedComplexity", "enum", true, "Suggested actor bucket."),
                    field("actors[].suggestedWeight", "integer", true, "Suggested actor weight."),
                    field("useCases[].useCaseId", "string", true, "Stable use case identifier."),
                    field("useCases[].useCaseName", "string", true, "Use case name from XML."),
                    field("useCases[].entityCount", "integer", false, "Detected or manually missing entity count."),
                    field("useCases[].stepCount", "integer", false, "Detected or manually missing step count."),
                    field("useCases[].classCount", "integer", false, "Detected or manually missing class count."),
                    field("useCases[].suggestedComplexity", "enum", true, "Suggested use case bucket."),
                    field("useCases[].suggestedWeight", "integer", true, "Suggested use case weight."),
                    field("relationships[].relationshipType", "enum", true, "Association, include, or extend."),
                    field("relationships[].sourceId", "string", true, "Edge start identifier."),
                    field("relationships[].targetId", "string", true, "Edge end identifier."),
                    field("pendingFields[].fieldKey", "string", true, "Missing manual input key."),
                    field("pendingFields[].reason", "string", true, "Why manual confirmation is required."),
                    field("evidence[].code", "string", true, "Evidence identifier."),
                    field("evidence[].evidenceType", "enum", true, "XML object, relation, rule hint, or manual override."),
                    field("processDetails.cards[].stepKey", "string", true, "Preview step key for card UI."),
                    field("processDetails.cards[].title", "string", true, "Preview card title."),
                    field("processDetails.tables[].tableKey", "string", true, "Preview table key."),
                    field("processDetails.tables[].columns[].key", "string", true, "Editable table column key.")
            ),
            List.of(
                    step("load-xml", "Load XML", "Validate source type, encoding, and root nodes."),
                    step("extract-actors", "Extract actors", "Collect Actor nodes and normalized stereotypes."),
                    step("extract-use-cases", "Extract use cases", "Collect UseCase nodes and parse associations."),
                    step("build-relations", "Build relations", "Link associations, include, and extend edges."),
                    step("suggest-complexity", "Suggest complexity", "Produce default weights and pending manual fields.")
            ),
            List.of(
                    table("actor-classification", "Actor classification", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("actorName", "Actor", "string", true, false),
                                    column("stereotype", "Stereotype", "string", false, false),
                                    column("suggestedComplexity", "Suggested complexity", "enum", true, true),
                                    column("suggestedWeight", "Weight", "integer", true, false)
                            ),
                            "Supports card and table rendering for actor review."),
                    table("use-case-complexity", "Use case complexity", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("useCaseName", "Use case", "string", true, false),
                                    column("entityCount", "Entity count", "integer", false, true),
                                    column("stepCount", "Step count", "integer", false, true),
                                    column("classCount", "Class count", "integer", false, true),
                                    column("suggestedComplexity", "Suggested complexity", "enum", true, true),
                                    column("suggestedWeight", "Weight", "integer", true, false)
                            ),
                            "Captures the three PPT heuristics used to classify use cases.")
            ),
            UCP_FORMULAS
    );

    public static final EndpointContract UCP_CALCULATE_CONTRACT = new EndpointContract(
            ModuleKey.UCP,
            ContractKind.CALCULATION,
            "POST /api/metrics/ucp/calculate",
            SourceModelType.POWERDESIGNER_USE_CASE,
            "Calculate UCP with traceable actor, use-case, TCF, EF, and effort breakdowns.",
            List.of(
                    field("projectName", "string", true, "Display name for the current analysis."),
                    field("actors[].selectedComplexity", "enum", true, "Confirmed actor complexity."),
                    field("actors[].selectedWeight", "integer", true, "Confirmed actor weight."),
                    field("useCases[].selectedComplexity", "enum", true, "Confirmed use case complexity."),
                    field("useCases[].selectedWeight", "integer", true, "Confirmed use case weight."),
                    field("technicalFactors[].code", "string", true, "Technical factor code T1-T13."),
                    field("technicalFactors[].score", "integer", true, "Factor score in the range 0-5."),
                    field("environmentalFactors[].code", "string", true, "Environmental factor code E1-E8."),
                    field("environmentalFactors[].score", "integer", true, "Factor score in the range 0-5."),
                    field("productivity", "decimal", true, "Defaults to 28 person-hours per UCP."),
                    field("productivityUnit", "string", true, "Defaults to person-hours-per-ucp.")
            ),
            List.of(
                    field("module", "enum", true, "Always UCP."),
                    field("contractKind", "enum", true, "Always CALCULATION."),
                    field("uaw", "decimal", true, "Unadjusted actor weight."),
                    field("uuc", "decimal", true, "Unadjusted use case weight."),
                    field("uucp", "decimal", true, "Unadjusted use case points."),
                    field("tcf", "decimal", true, "Technical complexity factor."),
                    field("ef", "decimal", true, "Environmental factor."),
                    field("ucp", "decimal", true, "Adjusted use case points."),
                    field("effort", "decimal", true, "Estimated effort."),
                    field("actorWeightBreakdown[].subtotal", "decimal", true, "Actor-level subtotal."),
                    field("useCaseWeightBreakdown[].subtotal", "decimal", true, "Use-case-level subtotal."),
                    field("tcfBreakdown[].weightedScore", "decimal", true, "Weighted technical score."),
                    field("efBreakdown[].weightedScore", "decimal", true, "Weighted environmental score."),
                    field("formulaTrace[].formulaKey", "string", true, "Formula identifier."),
                    field("formulaTrace[].expression", "string", true, "Expanded formula expression."),
                    field("formulaTrace[].resultField", "string", true, "Result field name."),
                    field("processDetails.cards[].stepKey", "string", true, "Calculation step card key."),
                    field("processDetails.tables[].tableKey", "string", true, "Calculation table key.")
            ),
            List.of(
                    step("calculate-uaw", "Calculate UAW", "Sum actor weights after confirmation."),
                    step("calculate-uuc", "Calculate UUC", "Sum use case weights after confirmation."),
                    step("calculate-uucp", "Calculate UUCP", "Apply UUCP = UAW + UUC."),
                    step("calculate-tcf", "Calculate TCF", "Apply the PPT technical factor formula."),
                    step("calculate-ef", "Calculate EF", "Apply the PPT environmental factor formula."),
                    step("calculate-ucp", "Calculate UCP", "Apply UCP = UUCP * TCF * EF."),
                    step("estimate-effort", "Estimate effort", "Apply the productivity coefficient.")
            ),
            List.of(
                    table("actor-weight-breakdown", "Actor weight breakdown", TablePurpose.RESULT_BREAKDOWN, false,
                            List.of(
                                    column("actorName", "Actor", "string", true, false),
                                    column("selectedComplexity", "Complexity", "enum", true, false),
                                    column("selectedWeight", "Weight", "integer", true, false),
                                    column("subtotal", "Subtotal", "decimal", true, false)
                            ),
                            "Supports classroom step cards and result tables."),
                    table("use-case-weight-breakdown", "Use case weight breakdown", TablePurpose.RESULT_BREAKDOWN, false,
                            List.of(
                                    column("useCaseName", "Use case", "string", true, false),
                                    column("selectedComplexity", "Complexity", "enum", true, false),
                                    column("selectedWeight", "Weight", "integer", true, false),
                                    column("subtotal", "Subtotal", "decimal", true, false)
                            ),
                            "Keeps the confirmed use-case bucket visible to the front end."),
                    table("tcf-factors", "Technical factors", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("code", "Code", "string", true, false),
                                    column("label", "Factor", "string", true, false),
                                    column("weight", "Weight", "decimal", true, false),
                                    column("score", "Score", "integer", true, true),
                                    column("weightedScore", "Weighted score", "decimal", true, false)
                            ),
                            "Matches the PPT T1-T13 worksheet."),
                    table("ef-factors", "Environmental factors", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("code", "Code", "string", true, false),
                                    column("label", "Factor", "string", true, false),
                                    column("weight", "Weight", "decimal", true, false),
                                    column("score", "Score", "integer", true, true),
                                    column("weightedScore", "Weighted score", "decimal", true, false)
                            ),
                            "Matches the PPT E1-E8 worksheet.")
            ),
            UCP_FORMULAS
    );

    public static final EndpointContract FP_PARSE_PREVIEW_CONTRACT = new EndpointContract(
            ModuleKey.FP,
            ContractKind.PARSE_PREVIEW,
            "POST /api/metrics/fp/parse",
            SourceModelType.POWERDESIGNER_DFD,
            "Parse PowerDesigner DFD XML and emit candidate function components with evidence.",
            List.of(
                    field("projectName", "string", true, "Display name for the current analysis."),
                    field("sourceName", "string", true, "Original XML file name."),
                    field("sourceType", "enum", true, "Must be POWERDESIGNER_DFD."),
                    field("xmlContent", "string", true, "Raw PowerDesigner XML content.")
            ),
            List.of(
                    field("module", "enum", true, "Always FP."),
                    field("contractKind", "enum", true, "Always PARSE_PREVIEW."),
                    field("processes[].processId", "string", true, "Stable process identifier."),
                    field("externalEntities[].entityId", "string", true, "External entity identifier."),
                    field("dataStores[].storeId", "string", true, "Data store identifier."),
                    field("flows[].flowId", "string", true, "Data flow identifier."),
                    field("componentCandidates[].candidateId", "string", true, "Stable candidate identifier."),
                    field("componentCandidates[].componentType", "enum", true, "ILF, EIF, EI, EO, or EQ."),
                    field("componentCandidates[].det", "integer", false, "Draft DET value."),
                    field("componentCandidates[].ret", "integer", false, "Draft RET value."),
                    field("componentCandidates[].ftr", "integer", false, "Draft FTR value."),
                    field("componentCandidates[].evidenceCodes", "string[]", true, "Evidence chain for the candidate."),
                    field("pendingFields[].fieldKey", "string", true, "Missing manual confirmation key."),
                    field("pendingFields[].reason", "string", true, "Why manual confirmation is required."),
                    field("processDetails.cards[].stepKey", "string", true, "Preview step card key."),
                    field("processDetails.tables[].tableKey", "string", true, "Preview table key.")
            ),
            List.of(
                    step("load-xml", "Load XML", "Validate source type, encoding, and root nodes."),
                    step("extract-dfd-objects", "Extract DFD objects", "Collect processes, data stores, entities, and flows."),
                    step("resolve-flows", "Resolve flow graph", "Link source and target nodes for all flows."),
                    step("generate-candidates", "Generate candidates", "Infer ILF, EIF, EI, EO, and EQ candidates."),
                    step("mark-pending-fields", "Mark pending fields", "Flag DET, RET, and FTR items that require review.")
            ),
            List.of(
                    table("function-component-candidates", "Function component candidates", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("componentType", "Component type", "enum", true, true),
                                    column("name", "Candidate", "string", true, false),
                                    column("det", "DET", "integer", false, true),
                                    column("ret", "RET", "integer", false, true),
                                    column("ftr", "FTR", "integer", false, true),
                                    column("evidenceCodes", "Evidence", "string[]", true, false)
                            ),
                            "Supports the second-stage manual edit flow for FP."),
                    table("flow-evidence", "Flow evidence", TablePurpose.RESULT_BREAKDOWN, false,
                            List.of(
                                    column("flowId", "Flow", "string", true, false),
                                    column("sourceName", "Source", "string", true, false),
                                    column("targetName", "Target", "string", true, false),
                                    column("inference", "Inference", "string", true, false)
                            ),
                            "Explains why each FP candidate was generated.")
            ),
            FP_FORMULAS
    );

    public static final EndpointContract FP_CALCULATE_CONTRACT = new EndpointContract(
            ModuleKey.FP,
            ContractKind.CALCULATION,
            "POST /api/metrics/fp/calculate",
            SourceModelType.POWERDESIGNER_DFD,
            "Calculate UFP, VAF, FP, and LOC with traceable component and GSC breakdowns.",
            List.of(
                    field("projectName", "string", true, "Display name for the current analysis."),
                    field("components[].componentType", "enum", true, "ILF, EIF, EI, EO, or EQ."),
                    field("components[].complexityLevel", "enum", true, "Confirmed complexity bucket."),
                    field("components[].det", "integer", true, "Confirmed DET value."),
                    field("components[].ret", "integer", false, "Confirmed RET value when applicable."),
                    field("components[].ftr", "integer", false, "Confirmed FTR value when applicable."),
                    field("gscScores[].code", "string", true, "General system characteristic code G1-G14."),
                    field("gscScores[].score", "integer", true, "Factor score in the range 0-5."),
                    field("language.code", "string", true, "Programming language code."),
                    field("language.slocPerFp", "integer", true, "SLOC per FP coefficient.")
            ),
            List.of(
                    field("module", "enum", true, "Always FP."),
                    field("contractKind", "enum", true, "Always CALCULATION."),
                    field("ufp", "decimal", true, "Unadjusted function points."),
                    field("vaf", "decimal", true, "Value adjustment factor."),
                    field("fp", "decimal", true, "Adjusted function points."),
                    field("locEstimate", "integer", true, "Language-specific LOC estimate."),
                    field("componentBreakdown[].weight", "integer", true, "Component weight after matrix hit."),
                    field("complexityMatrixHit[].componentType", "enum", true, "Matrix component type."),
                    field("complexityMatrixHit[].complexityLevel", "enum", true, "Matrix complexity bucket."),
                    field("gscBreakdown[].weightedScore", "decimal", true, "GSC weighted score."),
                    field("ufpTrace[].expression", "string", true, "Expanded UFP expression."),
                    field("vafTrace[].expression", "string", true, "Expanded VAF expression."),
                    field("locTrace[].expression", "string", true, "Expanded LOC expression."),
                    field("processDetails.cards[].stepKey", "string", true, "Calculation step card key."),
                    field("processDetails.tables[].tableKey", "string", true, "Calculation table key.")
            ),
            List.of(
                    step("classify-components", "Classify components", "Map DET, RET, and FTR to complexity buckets."),
                    step("calculate-ufp", "Calculate UFP", "Sum the weighted component scores."),
                    step("calculate-vaf", "Calculate VAF", "Apply the PPT 14-factor adjustment formula."),
                    step("calculate-fp", "Calculate FP", "Apply FP = UFP * VAF."),
                    step("estimate-loc", "Estimate LOC", "Apply the selected SLOC per FP profile.")
            ),
            List.of(
                    table("component-breakdown", "Component breakdown", TablePurpose.RESULT_BREAKDOWN, false,
                            List.of(
                                    column("name", "Component", "string", true, false),
                                    column("componentType", "Type", "enum", true, false),
                                    column("complexityLevel", "Complexity", "enum", true, false),
                                    column("weight", "Weight", "integer", true, false)
                            ),
                            "Explains the UFP subtotal per component."),
                    table("complexity-matrix-input", "Complexity matrix input", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("componentType", "Type", "enum", true, false),
                                    column("det", "DET", "integer", true, true),
                                    column("ret", "RET", "integer", false, true),
                                    column("ftr", "FTR", "integer", false, true),
                                    column("complexityLevel", "Complexity", "enum", true, false)
                            ),
                            "Preserves the inputs required to hit the FP complexity matrix."),
                    table("gsc-breakdown", "General system characteristics", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("code", "Code", "string", true, false),
                                    column("label", "Characteristic", "string", true, false),
                                    column("score", "Score", "integer", true, true)
                            ),
                            "Matches the 14-point PPT GSC worksheet."),
                    table("language-profile", "Language conversion", TablePurpose.EDITABLE_INPUT, true,
                            List.of(
                                    column("code", "Language code", "string", true, false),
                                    column("label", "Language", "string", true, false),
                                    column("slocPerFp", "SLOC/FP", "integer", true, true)
                            ),
                            "Allows the front end to surface the LOC conversion profile.")
            ),
            FP_FORMULAS
    );

    private MetricContracts() {
    }

    private static FieldDefinition field(String path, String type, boolean required, String description) {
        return new FieldDefinition(path, type, required, description);
    }

    private static ProcessStepDefinition step(String stepKey, String title, String description) {
        return new ProcessStepDefinition(stepKey, title, description);
    }

    private static TableDefinition table(String tableKey, String title, TablePurpose purpose, boolean editable,
                                         List<ColumnDefinition> columns, String description) {
        return new TableDefinition(tableKey, title, purpose, editable, columns, description);
    }

    private static ColumnDefinition column(String key, String label, String valueType, boolean required,
                                           boolean editable) {
        return new ColumnDefinition(key, label, valueType, required, editable);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    public enum ModuleKey {
        UCP,
        FP
    }

    public enum ContractKind {
        PARSE_PREVIEW,
        CALCULATION
    }

    public enum SourceModelType {
        POWERDESIGNER_USE_CASE,
        POWERDESIGNER_DFD
    }

    public enum TablePurpose {
        EDITABLE_INPUT,
        RESULT_BREAKDOWN
    }

    public enum ComplexityLevel {
        SIMPLE,
        AVERAGE,
        COMPLEX
    }

    public enum FunctionComponentType {
        ILF,
        EIF,
        EI,
        EO,
        EQ
    }

    public enum EvidenceType {
        XML_OBJECT,
        XML_RELATION,
        RULE_HINT,
        MANUAL_OVERRIDE,
        FORMULA
    }

    public static final class EndpointContract {
        private final ModuleKey module;
        private final ContractKind contractKind;
        private final String endpoint;
        private final SourceModelType sourceModelType;
        private final String description;
        private final List<FieldDefinition> requestFields;
        private final List<FieldDefinition> responseFields;
        private final List<ProcessStepDefinition> processSteps;
        private final List<TableDefinition> tables;
        private final List<FormulaDefinition> formulas;

        public EndpointContract(ModuleKey module, ContractKind contractKind, String endpoint,
                                SourceModelType sourceModelType, String description,
                                List<FieldDefinition> requestFields, List<FieldDefinition> responseFields,
                                List<ProcessStepDefinition> processSteps, List<TableDefinition> tables,
                                List<FormulaDefinition> formulas) {
            this.module = module;
            this.contractKind = contractKind;
            this.endpoint = endpoint;
            this.sourceModelType = sourceModelType;
            this.description = description;
            this.requestFields = List.copyOf(requestFields);
            this.responseFields = List.copyOf(responseFields);
            this.processSteps = List.copyOf(processSteps);
            this.tables = List.copyOf(tables);
            this.formulas = List.copyOf(formulas);
        }

        public ModuleKey getModule() {
            return module;
        }

        public ContractKind getContractKind() {
            return contractKind;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public SourceModelType getSourceModelType() {
            return sourceModelType;
        }

        public String getDescription() {
            return description;
        }

        public List<FieldDefinition> getRequestFields() {
            return requestFields;
        }

        public List<FieldDefinition> getResponseFields() {
            return responseFields;
        }

        public List<ProcessStepDefinition> getProcessSteps() {
            return processSteps;
        }

        public List<TableDefinition> getTables() {
            return tables;
        }

        public List<FormulaDefinition> getFormulas() {
            return formulas;
        }
    }

    public static final class FieldDefinition {
        private final String path;
        private final String type;
        private final boolean required;
        private final String description;

        public FieldDefinition(String path, String type, boolean required, String description) {
            this.path = path;
            this.type = type;
            this.required = required;
            this.description = description;
        }

        public String getPath() {
            return path;
        }

        public String getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class ProcessStepDefinition {
        private final String stepKey;
        private final String title;
        private final String description;

        public ProcessStepDefinition(String stepKey, String title, String description) {
            this.stepKey = stepKey;
            this.title = title;
            this.description = description;
        }

        public String getStepKey() {
            return stepKey;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class TableDefinition {
        private final String tableKey;
        private final String title;
        private final TablePurpose purpose;
        private final boolean editable;
        private final List<ColumnDefinition> columns;
        private final String description;

        public TableDefinition(String tableKey, String title, TablePurpose purpose, boolean editable,
                               List<ColumnDefinition> columns, String description) {
            this.tableKey = tableKey;
            this.title = title;
            this.purpose = purpose;
            this.editable = editable;
            this.columns = List.copyOf(columns);
            this.description = description;
        }

        public String getTableKey() {
            return tableKey;
        }

        public String getTitle() {
            return title;
        }

        public TablePurpose getPurpose() {
            return purpose;
        }

        public boolean isEditable() {
            return editable;
        }

        public List<ColumnDefinition> getColumns() {
            return columns;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class ColumnDefinition {
        private final String key;
        private final String label;
        private final String valueType;
        private final boolean required;
        private final boolean editable;

        public ColumnDefinition(String key, String label, String valueType, boolean required, boolean editable) {
            this.key = key;
            this.label = label;
            this.valueType = valueType;
            this.required = required;
            this.editable = editable;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public String getValueType() {
            return valueType;
        }

        public boolean isRequired() {
            return required;
        }

        public boolean isEditable() {
            return editable;
        }
    }

    public static final class WeightedOption {
        private final String code;
        private final String label;
        private final ComplexityLevel complexityLevel;
        private final int weight;
        private final String description;

        public WeightedOption(String code, String label, ComplexityLevel complexityLevel, int weight,
                              String description) {
            this.code = code;
            this.label = label;
            this.complexityLevel = complexityLevel;
            this.weight = weight;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public ComplexityLevel getComplexityLevel() {
            return complexityLevel;
        }

        public int getWeight() {
            return weight;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class UseCaseRule {
        private final String basis;
        private final Integer minimum;
        private final Integer maximum;
        private final ComplexityLevel complexityLevel;
        private final int weight;

        public UseCaseRule(String basis, Integer minimum, Integer maximum,
                           ComplexityLevel complexityLevel, int weight) {
            this.basis = basis;
            this.minimum = minimum;
            this.maximum = maximum;
            this.complexityLevel = complexityLevel;
            this.weight = weight;
        }

        public String getBasis() {
            return basis;
        }

        public Integer getMinimum() {
            return minimum;
        }

        public Integer getMaximum() {
            return maximum;
        }

        public ComplexityLevel getComplexityLevel() {
            return complexityLevel;
        }

        public int getWeight() {
            return weight;
        }
    }

    public static final class FactorDefinition {
        private final String code;
        private final String label;
        private final BigDecimal weight;
        private final String source;

        public FactorDefinition(String code, String label, BigDecimal weight, String source) {
            this.code = code;
            this.label = label;
            this.weight = weight;
            this.source = source;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public String getSource() {
            return source;
        }
    }

    public static final class FormulaDefinition {
        private final String formulaKey;
        private final String expression;
        private final String resultField;
        private final String description;

        public FormulaDefinition(String formulaKey, String expression, String resultField, String description) {
            this.formulaKey = formulaKey;
            this.expression = expression;
            this.resultField = resultField;
            this.description = description;
        }

        public String getFormulaKey() {
            return formulaKey;
        }

        public String getExpression() {
            return expression;
        }

        public String getResultField() {
            return resultField;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class FunctionComponentWeight {
        private final FunctionComponentType componentType;
        private final ComplexityLevel complexityLevel;
        private final int weight;
        private final String complexityBasis;

        public FunctionComponentWeight(FunctionComponentType componentType, ComplexityLevel complexityLevel,
                                       int weight, String complexityBasis) {
            this.componentType = componentType;
            this.complexityLevel = complexityLevel;
            this.weight = weight;
            this.complexityBasis = complexityBasis;
        }

        public FunctionComponentType getComponentType() {
            return componentType;
        }

        public ComplexityLevel getComplexityLevel() {
            return complexityLevel;
        }

        public int getWeight() {
            return weight;
        }

        public String getComplexityBasis() {
            return complexityBasis;
        }
    }

    public static final class GscDefinition {
        private final String code;
        private final String label;
        private final String source;

        public GscDefinition(String code, String label, String source) {
            this.code = code;
            this.label = label;
            this.source = source;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getSource() {
            return source;
        }
    }

    public static final class LanguageProfile {
        private final String code;
        private final String label;
        private final int slocPerFp;
        private final boolean defaultProfile;

        public LanguageProfile(String code, String label, int slocPerFp, boolean defaultProfile) {
            this.code = code;
            this.label = label;
            this.slocPerFp = slocPerFp;
            this.defaultProfile = defaultProfile;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public int getSlocPerFp() {
            return slocPerFp;
        }

        public boolean isDefaultProfile() {
            return defaultProfile;
        }
    }
}
