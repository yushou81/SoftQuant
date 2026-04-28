package com.softquant.backend.metrics.shared.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricContractsTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void shouldExposeUcpCatalogAlignedWithPptFormulas() {
        assertThat(MetricContracts.UCP_ACTOR_COMPLEXITIES)
                .extracting(MetricContracts.WeightedOption::getWeight)
                .containsExactly(1, 2, 3);

        assertThat(MetricContracts.UCP_USE_CASE_RULES)
                .extracting(MetricContracts.UseCaseRule::getWeight)
                .contains(5, 10, 15);

        assertThat(MetricContracts.UCP_TECHNICAL_FACTORS).hasSize(13);
        assertThat(MetricContracts.UCP_ENVIRONMENTAL_FACTORS).hasSize(8);
        assertThat(MetricContracts.DEFAULT_UCP_PRODUCTIVITY.toPlainString()).isEqualTo("28");
        assertThat(MetricContracts.UCP_FORMULAS)
                .extracting(MetricContracts.FormulaDefinition::getExpression)
                .containsExactly(
                        "UAW = sum(actorWeight)",
                        "UUC = sum(useCaseWeight)",
                        "UUCP = UAW + UUC",
                        "TCF = 0.6 + 0.01 * TFactor",
                        "EF = 1.4 - 0.03 * EFactor",
                        "UCP = UUCP * TCF * EF",
                        "Effort = UCP * productivity"
                );
    }

    @Test
    void shouldExposeFpCatalogAlignedWithPptFormulas() {
        assertThat(MetricContracts.FP_COMPONENT_WEIGHTS).hasSize(15);
        assertThat(MetricContracts.FP_GENERAL_SYSTEM_CHARACTERISTICS).hasSize(14);
        assertThat(MetricContracts.FP_LANGUAGE_PROFILES)
                .singleElement()
                .satisfies(profile -> {
                    assertThat(profile.getCode()).isEqualTo("JAVA");
                    assertThat(profile.getSlocPerFp()).isEqualTo(60);
                    assertThat(profile.isDefaultProfile()).isTrue();
                });

        assertThat(MetricContracts.FP_FORMULAS)
                .extracting(MetricContracts.FormulaDefinition::getExpression)
                .containsExactly(
                        "UFP = sum(componentWeight)",
                        "VAF = 0.65 + 0.01 * sum(gscScore)",
                        "FP = UFP * VAF",
                        "LOC = FP * slocPerFp"
                );
    }

    @Test
    void shouldCoverAllRequiredContractEndpointsAndFields() {
        List<MetricContracts.EndpointContract> contracts = List.of(
                MetricContracts.UCP_PARSE_PREVIEW_CONTRACT,
                MetricContracts.UCP_CALCULATE_CONTRACT,
                MetricContracts.FP_PARSE_PREVIEW_CONTRACT,
                MetricContracts.FP_CALCULATE_CONTRACT
        );

        assertThat(contracts)
                .extracting(MetricContracts.EndpointContract::getEndpoint)
                .containsExactly(
                        "POST /api/metrics/ucp/parse",
                        "POST /api/metrics/ucp/calculate",
                        "POST /api/metrics/fp/parse",
                        "POST /api/metrics/fp/calculate"
                );

        assertThat(MetricContracts.UCP_PARSE_PREVIEW_CONTRACT.getResponseFields())
                .extracting(MetricContracts.FieldDefinition::getPath)
                .contains(
                        "actors[].suggestedWeight",
                        "useCases[].classCount",
                        "processDetails.tables[].columns[].key"
                );

        assertThat(MetricContracts.UCP_CALCULATE_CONTRACT.getResponseFields())
                .extracting(MetricContracts.FieldDefinition::getPath)
                .contains(
                        "actorWeightBreakdown[].subtotal",
                        "formulaTrace[].expression",
                        "tcfBreakdown[].weightedScore"
                );

        assertThat(MetricContracts.FP_PARSE_PREVIEW_CONTRACT.getResponseFields())
                .extracting(MetricContracts.FieldDefinition::getPath)
                .contains(
                        "componentCandidates[].componentType",
                        "componentCandidates[].det",
                        "componentCandidates[].evidenceCodes"
                );

        assertThat(MetricContracts.FP_CALCULATE_CONTRACT.getResponseFields())
                .extracting(MetricContracts.FieldDefinition::getPath)
                .contains(
                        "componentBreakdown[].weight",
                        "complexityMatrixHit[].complexityLevel",
                        "locTrace[].expression"
                );
    }

    @Test
    void shouldRemainMachineReadableForFutureFrontendSharing() throws Exception {
        String json = jsonMapper.writeValueAsString(MetricContracts.UCP_CALCULATE_CONTRACT);

        assertThat(json).contains("\"module\":\"UCP\"");
        assertThat(json).contains("\"contractKind\":\"CALCULATION\"");
        assertThat(json).contains("\"actorWeightBreakdown[].subtotal\"");
        assertThat(json).contains("\"tcf-factors\"");
    }
}
