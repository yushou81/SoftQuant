package com.softquant.backend.metrics.fp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softquant.backend.metrics.fp.dto.FpCalculateRequest;
import jakarta.validation.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FpCalculateServiceTest {

    private final FpCalculateService service = new FpCalculateService();

    @Test
    void shouldCalculateFpAndLocUsingConfirmedInputs() {
        var response = service.calculate(new FpCalculateRequest(
                "数据流图1",
                List.of(
                        new FpCalculateRequest.ComponentInput("c1", "ILF", "消息", 20, 2, null, null),
                        new FpCalculateRequest.ComponentInput("c2", "EO", "生成消息 输出", 2, null, 1, null),
                        new FpCalculateRequest.ComponentInput("c3", "EQ", "查看消息 查询", 1, null, 1, null)
                ),
                List.of(
                        new FpCalculateRequest.GscScoreInput("G1", 3),
                        new FpCalculateRequest.GscScoreInput("G2", 2),
                        new FpCalculateRequest.GscScoreInput("G3", 2),
                        new FpCalculateRequest.GscScoreInput("G4", 1),
                        new FpCalculateRequest.GscScoreInput("G5", 2),
                        new FpCalculateRequest.GscScoreInput("G6", 3),
                        new FpCalculateRequest.GscScoreInput("G7", 2),
                        new FpCalculateRequest.GscScoreInput("G8", 1),
                        new FpCalculateRequest.GscScoreInput("G9", 2),
                        new FpCalculateRequest.GscScoreInput("G10", 1),
                        new FpCalculateRequest.GscScoreInput("G11", 1),
                        new FpCalculateRequest.GscScoreInput("G12", 1),
                        new FpCalculateRequest.GscScoreInput("G13", 1),
                        new FpCalculateRequest.GscScoreInput("G14", 1)
                ),
                new FpCalculateRequest.LanguageInput("JAVA", null)
        ));

        assertThat(response.getModule()).isEqualTo("FP");
        assertThat(response.getContractKind()).isEqualTo("CALCULATION");
        assertThat(response.getUfp()).isEqualByComparingTo("17.0000");
        assertThat(response.getVaf()).isEqualByComparingTo("0.8800");
        assertThat(response.getFp()).isEqualByComparingTo("14.9600");
        assertThat(response.getLocEstimate()).isEqualTo(898);
        assertThat(response.getLanguage().getCode()).isEqualTo("JAVA");
        assertThat(response.getLanguage().getSlocPerFp()).isEqualTo(60);
        assertThat(response.getComponentBreakdown()).hasSize(3);
        assertThat(response.getComplexityMatrixHit()).hasSize(3);
        assertThat(response.getGscBreakdown()).hasSize(14);
        assertThat(response.getUfpTrace()).hasSize(1);
        assertThat(response.getVafTrace()).hasSize(2);
        assertThat(response.getLocTrace()).hasSize(1);
    }

    @Test
    void shouldSupportManualComplexityAndLanguageOverride() {
        var response = service.calculate(new FpCalculateRequest(
                "覆盖场景",
                List.of(
                        new FpCalculateRequest.ComponentInput("c1", "EIF", "外部接口", 5, 1, null, "AVERAGE")
                ),
                List.of(),
                new FpCalculateRequest.LanguageInput("JAVA", 70)
        ));

        assertThat(response.getUfp()).isEqualByComparingTo("7.0000");
        assertThat(response.getVaf()).isEqualByComparingTo("0.6500");
        assertThat(response.getFp()).isEqualByComparingTo("4.5500");
        assertThat(response.getLocEstimate()).isEqualTo(319);
        assertThat(response.getLanguage().getSlocPerFp()).isEqualTo(70);
        assertThat(response.getComponentBreakdown().get(0).getComplexityLevel()).isEqualTo("AVERAGE");
        assertThat(response.getComponentBreakdown().get(0).getWeight()).isEqualTo(7);
    }

    @Test
    void shouldRejectUnknownLanguageAndInvalidInputs() {
        assertThatThrownBy(() -> service.calculate(new FpCalculateRequest(
                "非法语言",
                List.of(
                        new FpCalculateRequest.ComponentInput("c1", "ILF", "消息", 1, 1, null, null)
                ),
                List.of(),
                new FpCalculateRequest.LanguageInput("GO", 50)
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown language code");

        assertThatThrownBy(() -> service.calculate(new FpCalculateRequest(
                "非法组件",
                List.of(
                        new FpCalculateRequest.ComponentInput("c1", "ILF", "消息", null, 1, null, null)
                ),
                List.of(),
                new FpCalculateRequest.LanguageInput("JAVA", 60)
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("det must be provided");
    }
}
