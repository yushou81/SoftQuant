package com.softquant.backend.metrics.loc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softquant.backend.metrics.loc.dto.LocAnalyzeRequest;
import com.softquant.backend.metrics.loc.dto.LocAnalyzeResponse;
import com.softquant.backend.metrics.loc.dto.LocFileMetrics;
import com.softquant.backend.metrics.loc.service.LocAnalyzeService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LocMetricsController.class)
class LocMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocAnalyzeService locAnalyzeService;

    @Test
    void shouldExposeLocAnalyzeEndpoint() throws Exception {
        LocAnalyzeResponse response = new LocAnalyzeResponse();
        response.setModule("LOC");
        response.setContractKind("ANALYSIS");
        response.setProjectName("LOC Demo");
        response.setFileCount(1);
        response.setPhysicalLines(4);
        response.setCodeLines(3);
        response.setCommentLines(1);
        response.setBlankLines(0);
        response.setMixedLines(1);
        response.setLogicalLines(2);
        response.setCommentRate(new BigDecimal("50.00"));
        response.setFiles(List.of(file("Demo.java")));

        when(locAnalyzeService.analyze(any(LocAnalyzeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/metrics/loc/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocAnalyzeRequest(
                                "LOC Demo",
                                List.of(new LocAnalyzeRequest.SourceInput("Demo.java", "JAVA", "class Demo {}"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("LOC"))
                .andExpect(jsonPath("$.contractKind").value("ANALYSIS"))
                .andExpect(jsonPath("$.projectName").value("LOC Demo"))
                .andExpect(jsonPath("$.fileCount").value(1))
                .andExpect(jsonPath("$.physicalLines").value(4))
                .andExpect(jsonPath("$.files[0].fileName").value("Demo.java"))
                .andExpect(jsonPath("$.files[0].language").value("JAVA"));
    }

    @Test
    void shouldRejectEmptySourceList() throws Exception {
        mockMvc.perform(post("/api/metrics/loc/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocAnalyzeRequest("LOC Demo", List.of()))))
                .andExpect(status().isBadRequest());
    }

    private LocFileMetrics file(String fileName) {
        LocFileMetrics file = new LocFileMetrics();
        file.setFileName(fileName);
        file.setLanguage("JAVA");
        file.setPhysicalLines(4);
        file.setCodeLines(3);
        file.setCommentLines(1);
        file.setMixedLines(1);
        file.setLogicalLines(2);
        file.setCommentRate(new BigDecimal("50.00"));
        file.setParseStatus("AST_PARSED");
        return file;
    }
}
