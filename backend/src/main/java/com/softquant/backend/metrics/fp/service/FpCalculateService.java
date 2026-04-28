package com.softquant.backend.metrics.fp.service;

import com.softquant.backend.metrics.fp.dto.FpCalculateRequest;
import com.softquant.backend.metrics.fp.dto.FpCalculateResponse;
import com.softquant.backend.metrics.fp.dto.FpParseResponse;
import com.softquant.backend.metrics.shared.contract.MetricContracts;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FpCalculateService {

    private final Map<String, MetricContracts.FunctionComponentWeight> componentWeightMap;
    private final Map<String, MetricContracts.GscDefinition> gscDefinitionMap;
    private final Map<String, MetricContracts.LanguageProfile> languageProfileMap;

    public FpCalculateService() {
        this.componentWeightMap = MetricContracts.FP_COMPONENT_WEIGHTS.stream()
                .collect(Collectors.toMap(
                        weight -> key(weight.getComponentType().name(), weight.getComplexityLevel().name()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        this.gscDefinitionMap = MetricContracts.FP_GENERAL_SYSTEM_CHARACTERISTICS.stream()
                .collect(Collectors.toMap(
                        definition -> definition.getCode().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        this.languageProfileMap = MetricContracts.FP_LANGUAGE_PROFILES.stream()
                .collect(Collectors.toMap(
                        profile -> profile.getCode().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public FpCalculateResponse calculate(FpCalculateRequest request) {
        FpCalculateResponse.LanguageProfile language = resolveLanguage(request.language());
        List<FpCalculateResponse.ComponentBreakdown> componentBreakdown = buildComponentBreakdown(request.components());
        List<FpCalculateResponse.ComplexityMatrixHit> matrixHits = buildMatrixHits(componentBreakdown);
        List<FpCalculateResponse.GscBreakdown> gscBreakdown = buildGscBreakdown(request.gscScores());

        BigDecimal ufp = componentBreakdown.stream()
                .map(FpCalculateResponse.ComponentBreakdown::getWeight)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gscSum = gscBreakdown.stream()
                .map(FpCalculateResponse.GscBreakdown::getScore)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vaf = new BigDecimal("0.65").add(new BigDecimal("0.01").multiply(gscSum));
        BigDecimal fp = ufp.multiply(vaf);
        int locEstimate = scale(fp).multiply(BigDecimal.valueOf(language.getSlocPerFp()))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        FpCalculateResponse response = new FpCalculateResponse();
        response.setModule(MetricContracts.ModuleKey.FP.name());
        response.setContractKind(MetricContracts.ContractKind.CALCULATION.name());
        response.setProjectName(normalizeProjectName(request.projectName()));
        response.setUfp(scale(ufp));
        response.setVaf(scale(vaf));
        response.setFp(scale(fp));
        response.setLocEstimate(locEstimate);
        response.setLanguage(language);
        response.setComponentBreakdown(componentBreakdown);
        response.setComplexityMatrixHit(matrixHits);
        response.setGscBreakdown(gscBreakdown);
        response.setUfpTrace(buildUfpTrace(componentBreakdown, ufp));
        response.setVafTrace(buildVafTrace(gscBreakdown, gscSum, vaf, fp));
        response.setLocTrace(buildLocTrace(fp, language, locEstimate));
        response.setProcessDetails(buildProcessDetails());
        return response;
    }

    private List<FpCalculateResponse.ComponentBreakdown> buildComponentBreakdown(
            List<FpCalculateRequest.ComponentInput> components) {
        List<FpCalculateResponse.ComponentBreakdown> breakdown = new ArrayList<>();
        for (FpCalculateRequest.ComponentInput component : components) {
            validateComponent(component);

            String complexityLevel = component.complexityLevel() == null || component.complexityLevel().isBlank()
                    ? inferComplexity(component)
                    : component.complexityLevel().trim().toUpperCase(Locale.ROOT);
            MetricContracts.FunctionComponentWeight weight = componentWeightMap.get(
                    key(component.componentType(), complexityLevel)
            );
            if (weight == null) {
                throw new ValidationException("No FP weight found for " + component.componentType() + "/" + complexityLevel);
            }

            FpCalculateResponse.ComponentBreakdown item = new FpCalculateResponse.ComponentBreakdown();
            item.setCandidateId(component.candidateId());
            item.setComponentType(component.componentType().toUpperCase(Locale.ROOT));
            item.setName(component.name());
            item.setComplexityLevel(complexityLevel);
            item.setDet(component.det());
            item.setRet(component.ret());
            item.setFtr(component.ftr());
            item.setWeight(weight.getWeight());
            breakdown.add(item);
        }

        breakdown.sort(Comparator.comparing(FpCalculateResponse.ComponentBreakdown::getCandidateId));
        return breakdown;
    }

    private List<FpCalculateResponse.ComplexityMatrixHit> buildMatrixHits(
            List<FpCalculateResponse.ComponentBreakdown> breakdown) {
        List<FpCalculateResponse.ComplexityMatrixHit> hits = new ArrayList<>();
        for (FpCalculateResponse.ComponentBreakdown item : breakdown) {
            FpCalculateResponse.ComplexityMatrixHit hit = new FpCalculateResponse.ComplexityMatrixHit();
            hit.setCandidateId(item.getCandidateId());
            hit.setComponentType(item.getComponentType());
            hit.setComplexityLevel(item.getComplexityLevel());
            hit.setDet(item.getDet());
            hit.setRet(item.getRet());
            hit.setFtr(item.getFtr());
            hits.add(hit);
        }
        return hits;
    }

    private List<FpCalculateResponse.GscBreakdown> buildGscBreakdown(
            List<FpCalculateRequest.GscScoreInput> inputs) {
        Map<String, Integer> inputMap = new LinkedHashMap<>();
        if (inputs != null) {
            for (FpCalculateRequest.GscScoreInput input : inputs) {
                String code = input.code().trim().toUpperCase(Locale.ROOT);
                if (!gscDefinitionMap.containsKey(code)) {
                    throw new ValidationException("Unknown GSC code: " + input.code());
                }
                inputMap.put(code, input.score() == null ? 0 : input.score());
            }
        }

        List<FpCalculateResponse.GscBreakdown> breakdown = new ArrayList<>();
        for (MetricContracts.GscDefinition definition : gscDefinitionMap.values()) {
            FpCalculateResponse.GscBreakdown item = new FpCalculateResponse.GscBreakdown();
            item.setCode(definition.getCode());
            item.setLabel(definition.getLabel());
            item.setScore(inputMap.getOrDefault(definition.getCode(), 0));
            breakdown.add(item);
        }
        return breakdown;
    }

    private FpCalculateResponse.LanguageProfile resolveLanguage(FpCalculateRequest.LanguageInput input) {
        String code = input == null ? "JAVA" : input.code().trim().toUpperCase(Locale.ROOT);
        MetricContracts.LanguageProfile profile = languageProfileMap.get(code);
        if (profile == null) {
            throw new ValidationException("Unknown language code: " + code);
        }

        int slocPerFp = input != null && input.slocPerFp() != null
                ? input.slocPerFp()
                : profile.getSlocPerFp();
        if (slocPerFp <= 0) {
            throw new ValidationException("slocPerFp must be greater than 0");
        }

        FpCalculateResponse.LanguageProfile language = new FpCalculateResponse.LanguageProfile();
        language.setCode(profile.getCode());
        language.setLabel(profile.getLabel());
        language.setSlocPerFp(slocPerFp);
        return language;
    }

    private List<FpCalculateResponse.TraceLine> buildUfpTrace(
            List<FpCalculateResponse.ComponentBreakdown> breakdown,
            BigDecimal ufp) {
        List<FpCalculateResponse.TraceLine> traces = new ArrayList<>();
        String expression = breakdown.stream()
                .map(item -> item.getName() + "(" + item.getWeight() + ")")
                .collect(Collectors.joining(" + "));
        traces.add(trace("UFP", "UFP = " + expression, "ufp", ufp));
        return traces;
    }

    private List<FpCalculateResponse.TraceLine> buildVafTrace(
            List<FpCalculateResponse.GscBreakdown> gscBreakdown,
            BigDecimal gscSum,
            BigDecimal vaf,
            BigDecimal fp) {
        List<FpCalculateResponse.TraceLine> traces = new ArrayList<>();
        String gscExpression = gscBreakdown.stream()
                .map(item -> item.getCode() + "=" + item.getScore())
                .collect(Collectors.joining(", "));
        traces.add(trace("VAF", "GSC = [" + gscExpression + "], VAF = 0.65 + 0.01 * "
                + gscSum.toPlainString(), "vaf", vaf));
        traces.add(trace("FP", "FP = UFP * VAF", "fp", fp));
        return traces;
    }

    private List<FpCalculateResponse.TraceLine> buildLocTrace(
            BigDecimal fp,
            FpCalculateResponse.LanguageProfile language,
            Integer locEstimate) {
        List<FpCalculateResponse.TraceLine> traces = new ArrayList<>();
        traces.add(trace("LOC", "LOC = FP * " + language.getSlocPerFp() + " (" + language.getCode() + ")",
                "locEstimate", BigDecimal.valueOf(locEstimate)));
        return traces;
    }

    private FpParseResponse.ProcessDetails buildProcessDetails() {
        FpParseResponse.ProcessDetails details = new FpParseResponse.ProcessDetails();
        details.setCards(List.of(
                stepCard("classify-components", "判定复杂度", "按 DET/RET/FTR 命中复杂度矩阵。"),
                stepCard("calculate-ufp", "计算 UFP", "按复杂度权重求和得到未调整功能点。"),
                stepCard("calculate-vaf", "计算 VAF", "按 14 个 GSC 分值求得修正因子。"),
                stepCard("calculate-fp", "计算 FP", "按 UFP * VAF 得到最终功能点。"),
                stepCard("estimate-loc", "估算 LOC", "按语言换算系数估算代码行数。")
        ));
        details.setTables(List.of(
                table("component-breakdown", "组件明细", List.of(
                        column("componentType", "组件类型", "enum", true, false),
                        column("name", "组件名称", "string", true, false),
                        column("complexityLevel", "复杂度", "enum", true, true),
                        column("weight", "权重", "integer", true, false)
                )),
                table("complexity-matrix-input", "复杂度矩阵输入", List.of(
                        column("det", "DET", "integer", true, true),
                        column("ret", "RET", "integer", false, true),
                        column("ftr", "FTR", "integer", false, true),
                        column("complexityLevel", "复杂度", "enum", true, false)
                )),
                table("gsc-breakdown", "GSC 评分表", List.of(
                        column("code", "编号", "string", true, false),
                        column("label", "名称", "string", true, false),
                        column("score", "分值", "integer", true, true)
                )),
                table("language-profile", "语言换算表", List.of(
                        column("code", "语言", "string", true, false),
                        column("slocPerFp", "SLOC/FP", "integer", true, true)
                ))
        ));
        return details;
    }

    private String inferComplexity(FpCalculateRequest.ComponentInput component) {
        String type = component.componentType().trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "ILF", "EIF" -> inferDataFunctionComplexity(component.det(), component.ret());
            case "EI" -> inferEiComplexity(component.det(), component.ftr());
            case "EO", "EQ" -> inferEoEqComplexity(component.det(), component.ftr());
            default -> throw new ValidationException("Unknown componentType: " + component.componentType());
        };
    }

    private String inferDataFunctionComplexity(Integer det, Integer ret) {
        if (det == null || ret == null) {
            throw new ValidationException("ILF/EIF require det and ret");
        }

        boolean lowDet = det <= 19;
        boolean midDet = det >= 20 && det <= 50;
        boolean highDet = det >= 51;

        if (ret == 1) {
            if (lowDet) {
                return "SIMPLE";
            }
            if (midDet) {
                return "SIMPLE";
            }
            return "AVERAGE";
        }

        if (ret >= 2 && ret <= 5) {
            if (lowDet) {
                return "SIMPLE";
            }
            if (midDet) {
                return "AVERAGE";
            }
            return "COMPLEX";
        }

        if (lowDet) {
            return "AVERAGE";
        }
        if (midDet) {
            return "COMPLEX";
        }
        return "COMPLEX";
    }

    private String inferEiComplexity(Integer det, Integer ftr) {
        if (det == null || ftr == null) {
            throw new ValidationException("EI require det and ftr");
        }
        if (ftr <= 1) {
            if (det <= 4) {
                return "SIMPLE";
            }
            if (det <= 15) {
                return "SIMPLE";
            }
            return "AVERAGE";
        }
        if (ftr == 2) {
            if (det <= 4) {
                return "SIMPLE";
            }
            if (det <= 15) {
                return "AVERAGE";
            }
            return "COMPLEX";
        }
        if (det <= 4) {
            return "AVERAGE";
        }
        if (det <= 15) {
            return "COMPLEX";
        }
        return "COMPLEX";
    }

    private String inferEoEqComplexity(Integer det, Integer ftr) {
        if (det == null || ftr == null) {
            throw new ValidationException("EO/EQ require det and ftr");
        }
        if (ftr <= 1) {
            if (det <= 5) {
                return "SIMPLE";
            }
            if (det <= 19) {
                return "SIMPLE";
            }
            return "AVERAGE";
        }
        if (ftr <= 3) {
            if (det <= 5) {
                return "SIMPLE";
            }
            if (det <= 19) {
                return "AVERAGE";
            }
            return "COMPLEX";
        }
        if (det <= 5) {
            return "AVERAGE";
        }
        if (det <= 19) {
            return "COMPLEX";
        }
        return "COMPLEX";
    }

    private void validateComponent(FpCalculateRequest.ComponentInput component) {
        if (component.det() == null || component.det() < 0) {
            throw new ValidationException("det must be provided and >= 0");
        }
        if (component.ret() != null && component.ret() < 0) {
            throw new ValidationException("ret must be >= 0");
        }
        if (component.ftr() != null && component.ftr() < 0) {
            throw new ValidationException("ftr must be >= 0");
        }
    }

    private String key(String componentType, String complexityLevel) {
        return componentType.trim().toUpperCase(Locale.ROOT) + "|" + complexityLevel.trim().toUpperCase(Locale.ROOT);
    }

    private FpCalculateResponse.TraceLine trace(String key, String expression, String resultField, BigDecimal result) {
        FpCalculateResponse.TraceLine trace = new FpCalculateResponse.TraceLine();
        trace.setKey(key);
        trace.setExpression(expression);
        trace.setResultField(resultField);
        trace.setResult(scale(result));
        return trace;
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

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return "unnamed-fp-project";
        }
        return projectName;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
