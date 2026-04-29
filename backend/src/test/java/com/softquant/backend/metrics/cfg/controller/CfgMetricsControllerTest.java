package com.softquant.backend.metrics.cfg.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeRequest;
import com.softquant.backend.metrics.cfg.dto.CfgAnalyzeResponse;
import com.softquant.backend.metrics.cfg.dto.MethodComplexityMetrics;
import com.softquant.backend.metrics.cfg.service.CfgAnalyzeService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CfgMetricsController.class)
class CfgMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CfgAnalyzeService cfgAnalyzeService;

    @Test
    void shouldExposeCfgAnalyzeEndpoint() throws Exception {
        CfgAnalyzeResponse response = new CfgAnalyzeResponse();
        response.setModule("CFG");
        response.setContractKind("ANALYSIS");
        response.setProjectName("CFG Demo");
        response.setFileCount(1);
        response.setClassCount(1);
        response.setMethodCount(1);
        response.setAvgComplexity(new BigDecimal("2.00"));
        response.setMaxComplexity(2);
        response.setLowRiskMethodCount(1);
        response.setMethods(List.of(method()));
        response.setParseIssues(List.of());

        when(cfgAnalyzeService.analyze(any(CfgAnalyzeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/metrics/cfg/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CfgAnalyzeRequest(
                                "CFG Demo",
                                List.of(new CfgAnalyzeRequest.SourceInput("Demo.java", "class Demo { void run() {} }"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("CFG"))
                .andExpect(jsonPath("$.contractKind").value("ANALYSIS"))
                .andExpect(jsonPath("$.methodCount").value(1))
                .andExpect(jsonPath("$.avgComplexity").value(2.0))
                .andExpect(jsonPath("$.methods[0].className").value("Demo"))
                .andExpect(jsonPath("$.methods[0].riskLevel").value("LOW"));
    }

    @Test
    void shouldRejectBlankSourceContent() throws Exception {
        mockMvc.perform(post("/api/metrics/cfg/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CfgAnalyzeRequest(
                                "CFG Demo",
                                List.of(new CfgAnalyzeRequest.SourceInput("Demo.java", ""))
                        ))))
                .andExpect(status().isBadRequest());
    }

    private MethodComplexityMetrics method() {
        MethodComplexityMetrics method = new MethodComplexityMetrics();
        method.setFileName("Demo.java");
        method.setClassName("Demo");
        method.setMethodName("run");
        method.setSignature("void run()");
        method.setMethodType("METHOD");
        method.setComplexity(2);
        method.setRiskLevel("LOW");
        method.setDecisionPointCount(1);
        return method;
    }
}
