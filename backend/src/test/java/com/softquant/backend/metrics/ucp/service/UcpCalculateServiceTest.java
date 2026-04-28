package com.softquant.backend.metrics.ucp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softquant.backend.metrics.ucp.dto.UcpCalculateRequest;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class UcpCalculateServiceTest {

    private final UcpCalculateService service = new UcpCalculateService();

    @Test
    void shouldCalculateUcpUsingConfirmedWeightsAndFactors() {
        UcpCalculateRequest request = new UcpCalculateRequest(
                "在线教学系统",
                List.of(
                        new UcpCalculateRequest.ActorInput("a1", "学生", "COMPLEX", null),
                        new UcpCalculateRequest.ActorInput("a2", "教师", "COMPLEX", null),
                        new UcpCalculateRequest.ActorInput("a3", "系统管理员", "COMPLEX", null)
                ),
                List.of(
                        new UcpCalculateRequest.UseCaseInput("u1", "系统登录", "AVERAGE", null, 2, 5, 6),
                        new UcpCalculateRequest.UseCaseInput("u2", "导入学生列表", "AVERAGE", null, 2, 6, 6),
                        new UcpCalculateRequest.UseCaseInput("u3", "导入教师列表", "AVERAGE", null, 2, 6, 6)
                ),
                List.of(
                        new UcpCalculateRequest.FactorScoreInput("T1", 5),
                        new UcpCalculateRequest.FactorScoreInput("T2", 4),
                        new UcpCalculateRequest.FactorScoreInput("T3", 2),
                        new UcpCalculateRequest.FactorScoreInput("T4", 4),
                        new UcpCalculateRequest.FactorScoreInput("T5", 5),
                        new UcpCalculateRequest.FactorScoreInput("T6", 4),
                        new UcpCalculateRequest.FactorScoreInput("T7", 4),
                        new UcpCalculateRequest.FactorScoreInput("T8", 4),
                        new UcpCalculateRequest.FactorScoreInput("T9", 2),
                        new UcpCalculateRequest.FactorScoreInput("T10", 2),
                        new UcpCalculateRequest.FactorScoreInput("T11", 2),
                        new UcpCalculateRequest.FactorScoreInput("T12", 2)
                ),
                List.of(
                        new UcpCalculateRequest.FactorScoreInput("E1", 3),
                        new UcpCalculateRequest.FactorScoreInput("E2", 3),
                        new UcpCalculateRequest.FactorScoreInput("E3", 3),
                        new UcpCalculateRequest.FactorScoreInput("E4", 5),
                        new UcpCalculateRequest.FactorScoreInput("E5", 3),
                        new UcpCalculateRequest.FactorScoreInput("E6", 3)
                ),
                null,
                null
        );

        var response = service.calculate(request);

        assertThat(response.getModule()).isEqualTo("UCP");
        assertThat(response.getContractKind()).isEqualTo("CALCULATION");
        assertThat(response.getUaw()).isEqualByComparingTo("9.0000");
        assertThat(response.getUuc()).isEqualByComparingTo("30.0000");
        assertThat(response.getUucp()).isEqualByComparingTo("39.0000");
        assertThat(response.getTcf()).isEqualByComparingTo("1.0500");
        assertThat(response.getEf()).isEqualByComparingTo("0.7850");
        assertThat(response.getUcp()).isEqualByComparingTo("32.1458");
        assertThat(response.getEffort()).isEqualByComparingTo("900.0810");
        assertThat(response.getProductivity()).isEqualByComparingTo("28");
        assertThat(response.getProductivityUnit()).isEqualTo("person-hours-per-ucp");
        assertThat(response.getActorWeightBreakdown()).hasSize(3);
        assertThat(response.getUseCaseWeightBreakdown()).hasSize(3);
        assertThat(response.getTcfBreakdown()).hasSize(13);
        assertThat(response.getEfBreakdown()).hasSize(8);
        assertThat(response.getFormulaTrace()).hasSize(7);
        assertThat(response.getExplanations()).hasSize(6);
    }

    @Test
    void shouldAllowManualWeightOverrideAndMissingFactorDefaults() {
        var response = service.calculate(new UcpCalculateRequest(
                "手工覆盖场景",
                List.of(new UcpCalculateRequest.ActorInput("a1", "外部系统", null, 2)),
                List.of(new UcpCalculateRequest.UseCaseInput("u1", "查看成绩", null, 5, null, null, null)),
                List.of(),
                List.of(),
                new BigDecimal("30"),
                "person-hours-per-ucp"
        ));

        assertThat(response.getUaw()).isEqualByComparingTo("2.0000");
        assertThat(response.getUuc()).isEqualByComparingTo("5.0000");
        assertThat(response.getUucp()).isEqualByComparingTo("7.0000");
        assertThat(response.getTcf()).isEqualByComparingTo("0.6000");
        assertThat(response.getEf()).isEqualByComparingTo("1.4000");
        assertThat(response.getUcp()).isEqualByComparingTo("5.8800");
        assertThat(response.getEffort()).isEqualByComparingTo("176.4000");
        assertThat(response.getTcfBreakdown())
                .allSatisfy(item -> assertThat(item.getScore()).isEqualTo(0));
        assertThat(response.getEfBreakdown())
                .allSatisfy(item -> assertThat(item.getScore()).isEqualTo(0));
    }

    @Test
    void shouldRejectInvalidWeightsAndFactorCodes() {
        assertThatThrownBy(() -> service.calculate(new UcpCalculateRequest(
                "非法权重",
                List.of(new UcpCalculateRequest.ActorInput("a1", "学生", null, 9)),
                List.of(new UcpCalculateRequest.UseCaseInput("u1", "系统登录", "AVERAGE", null, 2, 5, 6)),
                List.of(),
                List.of(),
                null,
                null
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("actor selectedWeight");

        assertThatThrownBy(() -> service.calculate(new UcpCalculateRequest(
                "非法因子",
                List.of(new UcpCalculateRequest.ActorInput("a1", "学生", "COMPLEX", null)),
                List.of(new UcpCalculateRequest.UseCaseInput("u1", "系统登录", "AVERAGE", null, 2, 5, 6)),
                List.of(new UcpCalculateRequest.FactorScoreInput("TX", 3)),
                List.of(),
                null,
                null
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown factor code");
    }
}
