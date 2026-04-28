package com.softquant.backend.metrics.shared.powerdesigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class PowerDesignerXmlSupportTest {

    private final PowerDesignerXmlSupport support = new PowerDesignerXmlSupport();

    @Test
    void shouldParseUseCaseModelAndResolveDependencies() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "用例图", "在线教学系统.xml"),
                StandardCharsets.UTF_8
        );

        PowerDesignerXmlSupport.ParsedDocument document = support.parse(xml, "CLD_OBJECT_MODEL");

        assertThat(document.signature()).isEqualTo("CLD_OBJECT_MODEL");
        assertThat(document.objectRegistry().size()).isGreaterThan(100);

        Element dependency = document.objectRegistry().requireById("o48");
        assertThat(document.requiredText(dependency, "Stereotype")).isEqualTo("include");

        Element object1 = document.requireChild(dependency, "Object1");
        Element object2 = document.requireChild(dependency, "Object2");
        Element sourceUseCase = document.refResolver().resolveRequired(document.requireChild(object1, "UseCase"), "UseCase");
        Element targetUseCase = document.refResolver().resolveRequired(document.requireChild(object2, "UseCase"), "UseCase");

        assertThat(sourceUseCase.getAttribute("Id")).isEqualTo("o84");
        assertThat(targetUseCase.getAttribute("Id")).isEqualTo("o92");
    }

    @Test
    void shouldParseDfdModelAndNormalizeSupportedStereotypes() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "数据流图", "数据流图1.xml"),
                StandardCharsets.UTF_8
        );

        PowerDesignerXmlSupport.ParsedDocument document = support.parse(xml, "BPM_MODEL_XML");

        Element organizationUnit = document.objectRegistry().requireById("o19");
        Element resource = document.objectRegistry().requireById("o21");
        Element resourceFlow = document.objectRegistry().requireById("o12");

        assertThat(document.normalizeStereotype(organizationUnit, Map.of(
                "EXTERNAL_ENTITY", "EXTERNAL_ENTITY",
                "DATA_STORE", "DATA_STORE"
        ))).isEqualTo("EXTERNAL_ENTITY");
        assertThat(document.normalizeStereotype(resource, Map.of(
                "EXTERNAL_ENTITY", "EXTERNAL_ENTITY",
                "DATA_STORE", "DATA_STORE"
        ))).isEqualTo("DATA_STORE");

        Element process = document.refResolver().resolveRequired(
                document.requireChild(document.requireChild(resourceFlow, "Process"), "Process"),
                "Process"
        );
        Element data = document.refResolver().resolveRequired(
                document.requireChild(document.requireChild(resourceFlow, "MessageFlow.Data"), "Data"),
                "Data"
        );

        assertThat(process.getAttribute("Id")).isEqualTo("o20");
        assertThat(data.getAttribute("Id")).isEqualTo("o26");
    }

    @Test
    void shouldFailFastForBrokenReferenceAndUnknownStereotype() throws Exception {
        String xml = Files.readString(
                Path.of("..", "sample", "用例图", "在线教学系统.xml"),
                StandardCharsets.UTF_8
        );

        PowerDesignerXmlSupport.ParsedDocument document = support.parse(xml, "CLD_OBJECT_MODEL");
        Element dependency = document.objectRegistry().requireById("o48");

        assertThatThrownBy(() -> document.refResolver().resolveRequiredId("missing-ref", "UseCase", "test"))
                .isInstanceOf(PowerDesignerXmlSupport.PowerDesignerXmlException.class)
                .hasMessageContaining("Unresolved XML Id reference");

        assertThatThrownBy(() -> document.normalizeStereotype(dependency, Map.of("EXTEND", "EXTEND")))
                .isInstanceOf(PowerDesignerXmlSupport.PowerDesignerXmlException.class)
                .hasMessageContaining("Unknown stereotype");
    }

    @Test
    void shouldFailFastForEmptyOrUnexpectedInput() {
        assertThatThrownBy(() -> support.parse("", "CLD_OBJECT_MODEL"))
                .isInstanceOf(PowerDesignerXmlSupport.PowerDesignerXmlException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> support.parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <?PowerDesigner signature="BPM_MODEL_XML"?>
                <Model xmlns:a="attribute" xmlns:c="collection" xmlns:o="object">
                  <o:RootObject Id="o1"/>
                </Model>
                """, "CLD_OBJECT_MODEL"))
                .isInstanceOf(PowerDesignerXmlSupport.PowerDesignerXmlException.class)
                .hasMessageContaining("Unexpected PowerDesigner signature");
    }
}
