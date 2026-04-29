package com.softquant.backend.metrics.cfg.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CfgAnalyzeServiceTest {

    private final CfgAnalyzeService service = new CfgAnalyzeService(
            new CyclomaticComplexityService(),
            new ControlFlowGraphBuilder()
    );

    @Test
    void shouldCalculateComplexityForEmptyMethod() {
        String source = String.join("\n",
                "class Simple {",
                "    void empty() {}",
                "}"
        );

        var response = service.analyze(new CfgAnalyzeRequest(
                "CFG Demo",
                List.of(new CfgAnalyzeRequest.SourceInput("Simple.java", source))
        ));

        assertThat(response.getModule()).isEqualTo("CFG");
        assertThat(response.getContractKind()).isEqualTo("ANALYSIS");
        assertThat(response.getProjectName()).isEqualTo("CFG Demo");
        assertThat(response.getFileCount()).isEqualTo(1);
        assertThat(response.getClassCount()).isEqualTo(1);
        assertThat(response.getMethodCount()).isEqualTo(1);
        assertThat(response.getAvgComplexity()).isEqualByComparingTo("1.00");
        assertThat(response.getMaxComplexity()).isEqualTo(1);
        assertThat(response.getLowRiskMethodCount()).isEqualTo(1);

        assertThat(response.getMethods()).singleElement().satisfies(method -> {
            assertThat(method.getClassName()).isEqualTo("Simple");
            assertThat(method.getMethodName()).isEqualTo("empty");
            assertThat(method.getComplexity()).isEqualTo(1);
            assertThat(method.getRiskLevel()).isEqualTo("LOW");
            assertThat(method.getDecisionPoints()).isEmpty();
            assertThat(method.getGraph().getNodes())
                    .extracting("type")
                    .containsExactly("ENTRY", "STATEMENT", "EXIT");
        });
    }

    @Test
    void shouldCountCompoundJavaDecisionPoints() {
        String source = String.join("\n",
                "class Demo {",
                "    int risky(int x) {",
                "        if (x > 0 && x < 10) {",
                "            x++;",
                "        } else if (x == 0) {",
                "            x = 1;",
                "        }",
                "        for (int i = 0; i < x; i++) {",
                "            while (x > i) {",
                "                break;",
                "            }",
                "        }",
                "        switch (x) {",
                "            case 1: return 1;",
                "            case 2: return 2;",
                "            default: return 0;",
                "        }",
                "    }",
                "}"
        );

        var response = service.analyze(new CfgAnalyzeRequest(
                "CFG Demo",
                List.of(new CfgAnalyzeRequest.SourceInput("Demo.java", source))
        ));

        assertThat(response.getMethods()).singleElement().satisfies(method -> {
            assertThat(method.getMethodName()).isEqualTo("risky");
            assertThat(method.getDecisionPointCount()).isEqualTo(7);
            assertThat(method.getComplexity()).isEqualTo(8);
            assertThat(method.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(method.getDecisionPoints())
                    .extracting("kind")
                    .contains("IF", "LOGICAL", "FOR", "WHILE", "SWITCH_CASE");
            assertThat(method.getGraph().getEdges())
                    .extracting("type")
                    .contains("TRUE", "FALSE", "LOOP_BACK", "CASE_BRANCH");
        });
        assertThat(response.getMediumRiskMethodCount()).isEqualTo(1);
        assertThat(response.getFormulaTrace())
                .extracting("label")
                .containsExactly("McCabe", "Decision Count", "Risk Threshold");
    }

    @Test
    void shouldCountCatchTernaryAndLogicalOperators() {
        String source = String.join("\n",
                "class Guard {",
                "    int guard(String value) {",
                "        try {",
                "            return value == null || value.isBlank() ? 0 : 1;",
                "        } catch (RuntimeException ex) {",
                "            return -1;",
                "        }",
                "    }",
                "}"
        );

        var response = service.analyze(new CfgAnalyzeRequest(
                null,
                List.of(new CfgAnalyzeRequest.SourceInput("Guard.java", source))
        ));

        assertThat(response.getProjectName()).isEqualTo("unnamed-project");
        assertThat(response.getMethods()).singleElement().satisfies(method -> {
            assertThat(method.getDecisionPointCount()).isEqualTo(3);
            assertThat(method.getComplexity()).isEqualTo(4);
            assertThat(method.getDecisionPoints())
                    .extracting("kind")
                    .containsExactly("LOGICAL", "TERNARY", "CATCH");
        });
    }

    @Test
    void shouldSummarizeConstructorsAndClasses() {
        String source = String.join("\n",
                "class Account {",
                "    Account(boolean enabled) {",
                "        if (enabled) {",
                "            enabled = false;",
                "        }",
                "    }",
                "    void save() {}",
                "}",
                "class Audit {",
                "    void write() {}",
                "}"
        );

        var response = service.analyze(new CfgAnalyzeRequest(
                "Summary",
                List.of(new CfgAnalyzeRequest.SourceInput("Account.java", source))
        ));

        assertThat(response.getClassCount()).isEqualTo(2);
        assertThat(response.getMethodCount()).isEqualTo(3);
        assertThat(response.getClassSummaries())
                .filteredOn(summary -> summary.getClassName().equals("Account"))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.getMethodCount()).isEqualTo(2);
                    assertThat(summary.getTotalComplexity()).isEqualTo(3);
                    assertThat(summary.getMaxComplexity()).isEqualTo(2);
                });
        assertThat(response.getMethods())
                .filteredOn(method -> method.getMethodType().equals("CONSTRUCTOR"))
                .singleElement()
                .satisfies(method -> assertThat(method.getMethodName()).isEqualTo("Account"));
    }

    @Test
    void shouldCollectParseIssuesAndContinueWithValidFiles() {
        String validSource = String.join("\n",
                "class Valid {",
                "    void ok() {}",
                "}"
        );

        var response = service.analyze(new CfgAnalyzeRequest(
                "Broken",
                List.of(
                        new CfgAnalyzeRequest.SourceInput("Valid.java", validSource),
                        new CfgAnalyzeRequest.SourceInput("Broken.java", "class Broken { void x( ")
                )
        ));

        assertThat(response.getFileCount()).isEqualTo(2);
        assertThat(response.getMethodCount()).isEqualTo(1);
        assertThat(response.getParseIssues())
                .singleElement()
                .asString()
                .contains("Broken.java");
    }
}
