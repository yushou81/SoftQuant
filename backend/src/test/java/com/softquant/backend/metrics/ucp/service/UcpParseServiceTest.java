package com.softquant.backend.metrics.ucp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softquant.backend.metrics.shared.powerdesigner.PowerDesignerXmlSupport;
import com.softquant.backend.metrics.ucp.dto.UcpParseRequest;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
import jakarta.validation.ValidationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UcpParseServiceTest {

    private final UcpParseService service = new UcpParseService(new PowerDesignerXmlSupport());

    @Test
    void shouldParseUseCaseModelIntoPreviewResponse() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "用例图", "在线教学系统.xml"),
                StandardCharsets.UTF_8
        );

        UcpParseResponse response = service.parse(new UcpParseRequest(
                "在线教学系统",
                "在线教学系统.xml",
                "POWERDESIGNER_USE_CASE",
                xml
        ));

        assertThat(response.getModule()).isEqualTo("UCP");
        assertThat(response.getContractKind()).isEqualTo("PARSE_PREVIEW");
        assertThat(response.getActors()).hasSize(3);
        assertThat(response.getUseCases()).hasSize(16);
        assertThat(response.getRelationships()).hasSize(26);
        assertThat(response.getPendingFields()).hasSize(48);
        assertThat(response.getEvidence()).hasSize(45);
        assertThat(response.getProcessDetails().getCards()).hasSize(5);
        assertThat(response.getProcessDetails().getTables()).hasSize(2);

        assertThat(response.getActors())
                .allSatisfy(actor -> {
                    assertThat(actor.getSuggestedComplexity()).isEqualTo("COMPLEX");
                    assertThat(actor.getSuggestedWeight()).isEqualTo(3);
                    assertThat(actor.getEvidenceCodes()).isNotEmpty();
                });

        assertThat(response.getUseCases())
                .allSatisfy(useCase -> {
                    assertThat(useCase.getEntityCount()).isNull();
                    assertThat(useCase.getStepCount()).isNull();
                    assertThat(useCase.getClassCount()).isNull();
                    assertThat(useCase.getSuggestedWeight()).isIn(5, 10, 15);
                });

        assertThat(response.getRelationships())
                .extracting(UcpParseResponse.UcpRelationshipPreview::getRelationshipType)
                .contains("ACTOR_ASSOCIATION", "INCLUDE", "EXTEND");
    }

    @Test
    void shouldRejectUnexpectedSourceType() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "用例图", "在线教学系统.xml"),
                StandardCharsets.UTF_8
        );

        assertThatThrownBy(() -> service.parse(new UcpParseRequest(
                "在线教学系统",
                "在线教学系统.xml",
                "POWERDESIGNER_DFD",
                xml
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sourceType must be POWERDESIGNER_USE_CASE");
    }
}
