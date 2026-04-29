package com.softquant.backend.metrics.fp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softquant.backend.metrics.fp.dto.FpParseRequest;
import com.softquant.backend.metrics.shared.powerdesigner.PowerDesignerXmlSupport;
import jakarta.validation.ValidationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FpParseServiceTest {

    private final FpParseService service = new FpParseService(new PowerDesignerXmlSupport());

    @Test
    void shouldParseDfdModelIntoCandidates() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "数据流图", "数据流图1.xml"),
                StandardCharsets.UTF_8
        );

        var response = service.parse(new FpParseRequest(
                "数据流图1",
                "数据流图1.xml",
                "POWERDESIGNER_DFD",
                xml
        ));

        assertThat(response.getModule()).isEqualTo("FP");
        assertThat(response.getContractKind()).isEqualTo("PARSE_PREVIEW");
        assertThat(response.getProcesses()).hasSize(3);
        assertThat(response.getExternalEntities()).hasSize(1);
        assertThat(response.getDataStores()).hasSize(1);
        assertThat(response.getDataElements()).hasSize(3);
        assertThat(response.getFlows()).hasSize(4);
        assertThat(response.getComponentCandidates()).hasSize(4);
        assertThat(response.getPendingFields()).hasSize(4);
        assertThat(response.getProcessDetails().getCards()).hasSize(5);
        assertThat(response.getProcessDetails().getTables()).hasSize(2);

        assertThat(response.getComponentCandidates())
                .extracting(candidate -> candidate.getComponentType() + ":" + candidate.getName())
                .contains(
                        "ILF:消息",
                        "EI:生成消息 输入",
                        "EO:查看消息列表 输出",
                        "EQ:查看消息 查询"
                );
        assertThat(response.getComponentCandidates())
                .anySatisfy(candidate -> {
                    assertThat(candidate.getComponentType()).isEqualTo("ILF");
                    assertThat(candidate.getName()).isEqualTo("消息");
                    assertThat(candidate.getDet()).isEqualTo(1);
                    assertThat(candidate.getRet()).isEqualTo(1);
                })
                .anySatisfy(candidate -> {
                    assertThat(candidate.getComponentType()).isEqualTo("EI");
                    assertThat(candidate.getName()).isEqualTo("生成消息 输入");
                    assertThat(candidate.getDet()).isEqualTo(2);
                    assertThat(candidate.getFtr()).isEqualTo(1);
                })
                .anySatisfy(candidate -> {
                    assertThat(candidate.getComponentType()).isEqualTo("EO");
                    assertThat(candidate.getName()).isEqualTo("查看消息列表 输出");
                    assertThat(candidate.getDet()).isEqualTo(1);
                    assertThat(candidate.getFtr()).isEqualTo(1);
                })
                .anySatisfy(candidate -> {
                    assertThat(candidate.getComponentType()).isEqualTo("EQ");
                    assertThat(candidate.getName()).isEqualTo("查看消息 查询");
                    assertThat(candidate.getDet()).isEqualTo(1);
                    assertThat(candidate.getFtr()).isEqualTo(1);
                });
    }

    @Test
    void shouldRejectUnexpectedSourceType() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "数据流图", "数据流图1.xml"),
                StandardCharsets.UTF_8
        );

        assertThatThrownBy(() -> service.parse(new FpParseRequest(
                "数据流图1",
                "数据流图1.xml",
                "POWERDESIGNER_USE_CASE",
                xml
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sourceType must be POWERDESIGNER_DFD");
    }
}
