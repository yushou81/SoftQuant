package com.softquant.backend.metrics.ucp.service;

import com.softquant.backend.metrics.shared.contract.MetricContracts;
import com.softquant.backend.metrics.ucp.dto.UcpCalculateRequest;
import com.softquant.backend.metrics.ucp.dto.UcpCalculateResponse;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
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
public class UcpCalculateService {

    private static final BigDecimal TCF_BASE = new BigDecimal("0.6");
    private static final BigDecimal TCF_RATIO = new BigDecimal("0.01");
    private static final BigDecimal EF_BASE = new BigDecimal("1.4");
    private static final BigDecimal EF_RATIO = new BigDecimal("0.03");

    private final Map<String, MetricContracts.FactorDefinition> technicalFactorMap;
    private final Map<String, MetricContracts.FactorDefinition> environmentalFactorMap;

    public UcpCalculateService() {
        this.technicalFactorMap = indexFactors(MetricContracts.UCP_TECHNICAL_FACTORS);
        this.environmentalFactorMap = indexFactors(MetricContracts.UCP_ENVIRONMENTAL_FACTORS);
    }

    public UcpCalculateResponse calculate(UcpCalculateRequest request) {
        BigDecimal productivity = resolveProductivity(request.productivity());
        String productivityUnit = request.productivityUnit() == null || request.productivityUnit().isBlank()
                ? MetricContracts.DEFAULT_UCP_PRODUCTIVITY_UNIT
                : request.productivityUnit();

        List<UcpCalculateResponse.ActorWeightBreakdown> actorBreakdown = buildActorBreakdown(request.actors());
        List<UcpCalculateResponse.UseCaseWeightBreakdown> useCaseBreakdown = buildUseCaseBreakdown(request.useCases());

        BigDecimal uaw = sumActorWeight(actorBreakdown);
        BigDecimal uuc = sumUseCaseWeight(useCaseBreakdown);
        BigDecimal uucp = uaw.add(uuc);

        List<UcpCalculateResponse.FactorBreakdown> tcfBreakdown =
                buildFactorBreakdown(request.technicalFactors(), technicalFactorMap);
        List<UcpCalculateResponse.FactorBreakdown> efBreakdown =
                buildFactorBreakdown(request.environmentalFactors(), environmentalFactorMap);

        BigDecimal technicalScore = sumFactorScore(tcfBreakdown);
        BigDecimal environmentalScore = sumFactorScore(efBreakdown);
        // 这里严格按 PPT 口径保留 TCF/EF 原公式，不额外引入经验修正项。
        BigDecimal tcf = TCF_BASE.add(TCF_RATIO.multiply(technicalScore));
        BigDecimal ef = EF_BASE.subtract(EF_RATIO.multiply(environmentalScore));
        BigDecimal ucp = uucp.multiply(tcf).multiply(ef);
        BigDecimal effort = ucp.multiply(productivity);

        UcpCalculateResponse response = new UcpCalculateResponse();
        response.setModule(MetricContracts.ModuleKey.UCP.name());
        response.setContractKind(MetricContracts.ContractKind.CALCULATION.name());
        response.setProjectName(normalizeProjectName(request.projectName()));
        response.setProductivity(productivity);
        response.setProductivityUnit(productivityUnit);
        response.setUaw(scale(uaw));
        response.setUuc(scale(uuc));
        response.setUucp(scale(uucp));
        response.setTcf(scale(tcf));
        response.setEf(scale(ef));
        response.setUcp(scale(ucp));
        response.setEffort(scale(effort));
        response.setActorWeightBreakdown(actorBreakdown);
        response.setUseCaseWeightBreakdown(useCaseBreakdown);
        response.setTcfBreakdown(tcfBreakdown);
        response.setEfBreakdown(efBreakdown);
        response.setFormulaTrace(buildFormulaTrace(
                uaw, uuc, uucp, technicalScore, tcf, environmentalScore, ef, ucp, productivity, effort
        ));
        response.setExplanations(buildExplanations(uaw, uuc, tcf, ef, ucp, effort, productivity, productivityUnit));
        response.setProcessDetails(buildProcessDetails());
        return response;
    }

    private List<UcpCalculateResponse.ActorWeightBreakdown> buildActorBreakdown(
            List<UcpCalculateRequest.ActorInput> actors) {
        List<UcpCalculateResponse.ActorWeightBreakdown> breakdown = new ArrayList<>();
        for (UcpCalculateRequest.ActorInput actor : actors) {
            int selectedWeight = resolveActorWeight(actor.selectedComplexity(), actor.selectedWeight());

            UcpCalculateResponse.ActorWeightBreakdown item = new UcpCalculateResponse.ActorWeightBreakdown();
            item.setActorId(actor.actorId());
            item.setActorName(actor.actorName());
            item.setSelectedComplexity(normalizeActorComplexity(actor.selectedComplexity(), selectedWeight));
            item.setSelectedWeight(selectedWeight);
            item.setSubtotal(BigDecimal.valueOf(selectedWeight));
            breakdown.add(item);
        }

        breakdown.sort(Comparator.comparing(UcpCalculateResponse.ActorWeightBreakdown::getActorName));
        return breakdown;
    }

    private List<UcpCalculateResponse.UseCaseWeightBreakdown> buildUseCaseBreakdown(
            List<UcpCalculateRequest.UseCaseInput> useCases) {
        List<UcpCalculateResponse.UseCaseWeightBreakdown> breakdown = new ArrayList<>();
        for (UcpCalculateRequest.UseCaseInput useCase : useCases) {
            int selectedWeight = resolveUseCaseWeight(useCase.selectedComplexity(), useCase.selectedWeight());

            UcpCalculateResponse.UseCaseWeightBreakdown item = new UcpCalculateResponse.UseCaseWeightBreakdown();
            item.setUseCaseId(useCase.useCaseId());
            item.setUseCaseName(useCase.useCaseName());
            item.setSelectedComplexity(normalizeUseCaseComplexity(useCase.selectedComplexity(), selectedWeight));
            item.setSelectedWeight(selectedWeight);
            item.setEntityCount(useCase.entityCount());
            item.setStepCount(useCase.stepCount());
            item.setClassCount(useCase.classCount());
            item.setSubtotal(BigDecimal.valueOf(selectedWeight));
            breakdown.add(item);
        }

        breakdown.sort(Comparator.comparing(UcpCalculateResponse.UseCaseWeightBreakdown::getUseCaseName));
        return breakdown;
    }

    private List<UcpCalculateResponse.FactorBreakdown> buildFactorBreakdown(
            List<UcpCalculateRequest.FactorScoreInput> inputs,
            Map<String, MetricContracts.FactorDefinition> factorMap) {
        Map<String, Integer> inputScoreMap = new LinkedHashMap<>();
        if (inputs != null) {
            for (UcpCalculateRequest.FactorScoreInput input : inputs) {
                MetricContracts.FactorDefinition factor = factorMap.get(normalizeCode(input.code()));
                if (factor == null) {
                    throw new ValidationException("Unknown factor code: " + input.code());
                }
                int score = input.score() == null ? 0 : input.score();
                inputScoreMap.put(factor.getCode(), score);
            }
        }

        List<UcpCalculateResponse.FactorBreakdown> breakdown = new ArrayList<>();
        for (MetricContracts.FactorDefinition factor : factorMap.values()) {
            int score = inputScoreMap.getOrDefault(factor.getCode(), 0);

            // 缺失因子默认补 0，便于前端只提交修改过的项目。
            UcpCalculateResponse.FactorBreakdown item = new UcpCalculateResponse.FactorBreakdown();
            item.setCode(factor.getCode());
            item.setLabel(factor.getLabel());
            item.setWeight(factor.getWeight());
            item.setScore(score);
            item.setWeightedScore(scale(factor.getWeight().multiply(BigDecimal.valueOf(score))));
            breakdown.add(item);
        }
        return breakdown;
    }

    private BigDecimal sumActorWeight(List<UcpCalculateResponse.ActorWeightBreakdown> breakdown) {
        return breakdown.stream()
                .map(UcpCalculateResponse.ActorWeightBreakdown::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumUseCaseWeight(List<UcpCalculateResponse.UseCaseWeightBreakdown> breakdown) {
        return breakdown.stream()
                .map(UcpCalculateResponse.UseCaseWeightBreakdown::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumFactorScore(List<UcpCalculateResponse.FactorBreakdown> breakdown) {
        return breakdown.stream()
                .map(UcpCalculateResponse.FactorBreakdown::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<UcpCalculateResponse.FormulaTrace> buildFormulaTrace(
            BigDecimal uaw,
            BigDecimal uuc,
            BigDecimal uucp,
            BigDecimal technicalScore,
            BigDecimal tcf,
            BigDecimal environmentalScore,
            BigDecimal ef,
            BigDecimal ucp,
            BigDecimal productivity,
            BigDecimal effort) {
        List<UcpCalculateResponse.FormulaTrace> traces = new ArrayList<>();
        traces.add(trace("UAW", "UAW = sum(actorWeight) = " + scale(uaw).toPlainString(), "uaw", uaw));
        traces.add(trace("UUC", "UUC = sum(useCaseWeight) = " + scale(uuc).toPlainString(), "uuc", uuc));
        traces.add(trace("UUCP", "UUCP = UAW + UUC = " + scale(uaw).toPlainString() + " + "
                + scale(uuc).toPlainString(), "uucp", uucp));
        traces.add(trace("TCF", "TCF = 0.6 + 0.01 * " + scale(technicalScore).toPlainString(), "tcf", tcf));
        traces.add(trace("EF", "EF = 1.4 - 0.03 * " + scale(environmentalScore).toPlainString(), "ef", ef));
        traces.add(trace("UCP", "UCP = " + scale(uucp).toPlainString() + " * " + scale(tcf).toPlainString()
                + " * " + scale(ef).toPlainString(), "ucp", ucp));
        traces.add(trace("EFFORT", "Effort = " + scale(ucp).toPlainString() + " * "
                + scale(productivity).toPlainString(), "effort", effort));
        return traces;
    }

    private List<UcpCalculateResponse.ExplanationLine> buildExplanations(
            BigDecimal uaw,
            BigDecimal uuc,
            BigDecimal tcf,
            BigDecimal ef,
            BigDecimal ucp,
            BigDecimal effort,
            BigDecimal productivity,
            String productivityUnit) {
        List<UcpCalculateResponse.ExplanationLine> explanations = new ArrayList<>();
        explanations.add(explanation("UAW", "UAW 由全部参与者的确认权重求和得到，当前结果为 "
                + scale(uaw).toPlainString() + "。"));
        explanations.add(explanation("UUC", "UUC 由全部用例的确认权重求和得到，当前结果为 "
                + scale(uuc).toPlainString() + "。"));
        explanations.add(explanation("TCF", "TCF 根据 13 个技术因子的加权和修正，当前结果为 "
                + scale(tcf).toPlainString() + "。"));
        explanations.add(explanation("EF", "EF 根据 8 个环境因子的加权和修正，当前结果为 "
                + scale(ef).toPlainString() + "。"));
        explanations.add(explanation("UCP", "UCP 为 UUCP、TCF、EF 三者乘积，当前结果为 "
                + scale(ucp).toPlainString() + "。"));
        explanations.add(explanation("EFFORT", "工作量按生产率 " + scale(productivity).toPlainString()
                + " " + productivityUnit + " 估算，当前结果为 " + scale(effort).toPlainString() + "。"));
        return explanations;
    }

    private UcpParseResponse.UcpProcessDetails buildProcessDetails() {
        UcpParseResponse.UcpProcessDetails details = new UcpParseResponse.UcpProcessDetails();
        details.setCards(List.of(
                stepCard("calculate-uaw", "计算 UAW", "汇总参与者确认权重。"),
                stepCard("calculate-uuc", "计算 UUC", "汇总用例确认权重。"),
                stepCard("calculate-uucp", "计算 UUCP", "按 UAW + UUC 得到未调整用例点。"),
                stepCard("calculate-tcf-ef", "计算 TCF / EF", "按 PPT 因子公式求得修正系数。"),
                stepCard("calculate-ucp-effort", "汇总 UCP 与工作量", "输出公式轨迹、总分和工作量。")
        ));
        details.setTables(List.of(
                table("actor-weight-breakdown", "Actor 权重明细", List.of(
                        column("actorName", "Actor", "string", true, false),
                        column("selectedComplexity", "复杂度", "enum", true, true),
                        column("selectedWeight", "权重", "integer", true, true),
                        column("subtotal", "小计", "decimal", true, false)
                )),
                table("use-case-weight-breakdown", "UseCase 权重明细", List.of(
                        column("useCaseName", "UseCase", "string", true, false),
                        column("selectedComplexity", "复杂度", "enum", true, true),
                        column("selectedWeight", "权重", "integer", true, true),
                        column("subtotal", "小计", "decimal", true, false)
                )),
                table("tcf-factors", "TCF 因子表", List.of(
                        column("code", "编号", "string", true, false),
                        column("label", "名称", "string", true, false),
                        column("weight", "权重", "decimal", true, false),
                        column("score", "取值", "integer", true, true),
                        column("weightedScore", "加权分", "decimal", true, false)
                )),
                table("ef-factors", "EF 因子表", List.of(
                        column("code", "编号", "string", true, false),
                        column("label", "名称", "string", true, false),
                        column("weight", "权重", "decimal", true, false),
                        column("score", "取值", "integer", true, true),
                        column("weightedScore", "加权分", "decimal", true, false)
                ))
        ));
        return details;
    }

    private int resolveActorWeight(String selectedComplexity, Integer selectedWeight) {
        // 人工覆盖优先，其次才退回到复杂度枚举映射，避免前端编辑值被二次改写。
        if (selectedWeight != null) {
            if (!List.of(1, 2, 3).contains(selectedWeight)) {
                throw new ValidationException("actor selectedWeight must be 1, 2, or 3");
            }
            return selectedWeight;
        }
        if (selectedComplexity == null || selectedComplexity.isBlank()) {
            throw new ValidationException("actor selectedComplexity or selectedWeight is required");
        }
        return switch (selectedComplexity.trim().toUpperCase(Locale.ROOT)) {
            case "SIMPLE" -> 1;
            case "AVERAGE" -> 2;
            case "COMPLEX" -> 3;
            default -> throw new ValidationException("unknown actor complexity: " + selectedComplexity);
        };
    }

    private int resolveUseCaseWeight(String selectedComplexity, Integer selectedWeight) {
        // UseCase 与 Actor 同理，优先采用用户确认后的权重。
        if (selectedWeight != null) {
            if (!List.of(5, 10, 15).contains(selectedWeight)) {
                throw new ValidationException("useCase selectedWeight must be 5, 10, or 15");
            }
            return selectedWeight;
        }
        if (selectedComplexity == null || selectedComplexity.isBlank()) {
            throw new ValidationException("useCase selectedComplexity or selectedWeight is required");
        }
        return switch (selectedComplexity.trim().toUpperCase(Locale.ROOT)) {
            case "SIMPLE" -> 5;
            case "AVERAGE" -> 10;
            case "COMPLEX" -> 15;
            default -> throw new ValidationException("unknown useCase complexity: " + selectedComplexity);
        };
    }

    private String normalizeActorComplexity(String selectedComplexity, int selectedWeight) {
        if (selectedComplexity != null && !selectedComplexity.isBlank()) {
            return selectedComplexity.trim().toUpperCase(Locale.ROOT);
        }
        return switch (selectedWeight) {
            case 1 -> "SIMPLE";
            case 2 -> "AVERAGE";
            default -> "COMPLEX";
        };
    }

    private String normalizeUseCaseComplexity(String selectedComplexity, int selectedWeight) {
        if (selectedComplexity != null && !selectedComplexity.isBlank()) {
            return selectedComplexity.trim().toUpperCase(Locale.ROOT);
        }
        return switch (selectedWeight) {
            case 5 -> "SIMPLE";
            case 10 -> "AVERAGE";
            default -> "COMPLEX";
        };
    }

    private BigDecimal resolveProductivity(BigDecimal productivity) {
        if (productivity == null) {
            return MetricContracts.DEFAULT_UCP_PRODUCTIVITY;
        }
        if (productivity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("productivity must be greater than 0");
        }
        return productivity;
    }

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return "unnamed-ucp-project";
        }
        return projectName;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, MetricContracts.FactorDefinition> indexFactors(
            List<MetricContracts.FactorDefinition> factors) {
        return factors.stream()
                .collect(Collectors.toMap(
                        factor -> factor.getCode().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private UcpCalculateResponse.FormulaTrace trace(
            String formulaKey,
            String expression,
            String resultField,
            BigDecimal result) {
        UcpCalculateResponse.FormulaTrace trace = new UcpCalculateResponse.FormulaTrace();
        trace.setFormulaKey(formulaKey);
        trace.setExpression(expression);
        trace.setResultField(resultField);
        trace.setResult(scale(result));
        return trace;
    }

    private UcpCalculateResponse.ExplanationLine explanation(String code, String text) {
        UcpCalculateResponse.ExplanationLine line = new UcpCalculateResponse.ExplanationLine();
        line.setCode(code);
        line.setText(text);
        return line;
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

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
