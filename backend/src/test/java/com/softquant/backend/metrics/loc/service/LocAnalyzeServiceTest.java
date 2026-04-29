package com.softquant.backend.metrics.loc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.softquant.backend.metrics.loc.dto.LocAnalyzeRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocAnalyzeServiceTest {

    private final LocAnalyzeService service = new LocAnalyzeService();

    @Test
    void shouldAnalyzeJavaPhysicalCommentAndLogicalLines() {
        String source = String.join("\n",
                "package demo;",
                "",
                "public class Demo {",
                "    private String text = \"not // comment\"; // field note",
                "    /*",
                "     * block comment",
                "     */",
                "    public int add(int a, int b) {",
                "        int total = a + b;",
                "        return total;",
                "    }",
                "}"
        );

        var response = service.analyze(new LocAnalyzeRequest(
                "LOC Demo",
                List.of(new LocAnalyzeRequest.SourceInput("Demo.java", null, source))
        ));

        assertThat(response.getModule()).isEqualTo("LOC");
        assertThat(response.getContractKind()).isEqualTo("ANALYSIS");
        assertThat(response.getProjectName()).isEqualTo("LOC Demo");
        assertThat(response.getFileCount()).isEqualTo(1);
        assertThat(response.getPhysicalLines()).isEqualTo(12);
        assertThat(response.getCodeLines()).isEqualTo(8);
        assertThat(response.getCommentLines()).isEqualTo(3);
        assertThat(response.getBlankLines()).isEqualTo(1);
        assertThat(response.getMixedLines()).isEqualTo(1);
        assertThat(response.getLogicalLines()).isEqualTo(5);
        assertThat(response.getCommentRate()).isEqualByComparingTo("36.36");

        assertThat(response.getFiles()).singleElement().satisfies(file -> {
            assertThat(file.getFileName()).isEqualTo("Demo.java");
            assertThat(file.getLanguage()).isEqualTo("JAVA");
            assertThat(file.getParseStatus()).isEqualTo("AST_PARSED");
        });
        assertThat(response.getFormulaTrace())
                .extracting("label")
                .containsExactly("Physical LOC", "Comment Rate", "Logical LOC");
    }

    @Test
    void shouldIgnoreCommentTokensInsideJavaStringsAndChars() {
        String source = String.join("\n",
                "class Trick {",
                "    String url = \"http://example.com\";",
                "    String block = \"/* not comment */\";",
                "    char slash = '/';",
                "}"
        );

        var response = service.analyze(new LocAnalyzeRequest(
                null,
                List.of(new LocAnalyzeRequest.SourceInput("Trick.java", "java", source))
        ));

        assertThat(response.getProjectName()).isEqualTo("unnamed-project");
        assertThat(response.getPhysicalLines()).isEqualTo(5);
        assertThat(response.getCodeLines()).isEqualTo(5);
        assertThat(response.getCommentLines()).isZero();
        assertThat(response.getMixedLines()).isZero();
        assertThat(response.getBlankLines()).isZero();
        assertThat(response.getCommentRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldAnalyzePythonHashCommentsAndDocstrings() {
        String source = String.join("\n",
                "\"\"\"module doc\"\"\"",
                "import os  # mixed",
                "",
                "def run():",
                "    text = \"# not comment\"",
                "    return text"
        );

        var response = service.analyze(new LocAnalyzeRequest(
                "Python Demo",
                List.of(new LocAnalyzeRequest.SourceInput("script.py", null, source))
        ));

        assertThat(response.getPhysicalLines()).isEqualTo(6);
        assertThat(response.getCodeLines()).isEqualTo(4);
        assertThat(response.getCommentLines()).isEqualTo(1);
        assertThat(response.getBlankLines()).isEqualTo(1);
        assertThat(response.getMixedLines()).isEqualTo(1);
        assertThat(response.getLogicalLines()).isEqualTo(4);
        assertThat(response.getCommentRate()).isEqualByComparingTo("40.00");
        assertThat(response.getFiles()).singleElement().satisfies(file -> {
            assertThat(file.getLanguage()).isEqualTo("PYTHON");
            assertThat(file.getParseStatus()).isEqualTo("TEXT_FALLBACK");
        });
    }

    @Test
    void shouldAggregateByLanguageAndHandleEmptyFiles() {
        String javaSource = String.join("\n",
                "class A {",
                "    void run() {}",
                "}"
        );
        String cppSource = String.join("\n",
                "// header",
                "int main() { return 0; }"
        );

        var response = service.analyze(new LocAnalyzeRequest(
                "Mixed Project",
                List.of(
                        new LocAnalyzeRequest.SourceInput("A.java", null, javaSource),
                        new LocAnalyzeRequest.SourceInput("main.cpp", null, cppSource),
                        new LocAnalyzeRequest.SourceInput("empty.txt", null, "")
                )
        ));

        assertThat(response.getFileCount()).isEqualTo(3);
        assertThat(response.getPhysicalLines()).isEqualTo(5);
        assertThat(response.getCodeLines()).isEqualTo(4);
        assertThat(response.getCommentLines()).isEqualTo(1);
        assertThat(response.getBlankLines()).isZero();
        assertThat(response.getLanguageSummaries())
                .extracting("language")
                .containsExactly("JAVA", "GENERIC", "CPP");
        assertThat(response.getLanguageSummaries())
                .filteredOn(summary -> summary.getLanguage().equals("GENERIC"))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.getFileCount()).isEqualTo(1);
                    assertThat(summary.getPhysicalLines()).isZero();
                });
    }
}
