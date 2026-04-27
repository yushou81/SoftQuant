package com.softquant.backend.metrics.fp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softquant.backend.metrics.fp.dto.FpCalculateRequest;
import com.softquant.backend.metrics.fp.dto.FpCalculateResponse;
import com.softquant.backend.metrics.fp.dto.FpParseRequest;
import com.softquant.backend.metrics.fp.dto.FpParseResponse;
import com.softquant.backend.metrics.fp.service.FpCalculateService;
import com.softquant.backend.metrics.fp.service.FpParseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FpMetricsController.class)
class FpMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FpParseService fpParseService;

    @MockBean
    private FpCalculateService fpCalculateService;

    @Test
    void shouldExposeFpParseEndpoint() throws Exception {
        FpParseResponse response = new FpParseResponse();
        response.setModule("FP");
        response.setContractKind("PARSE_PREVIEW");
        response.setProjectName("数据流图1");
        response.setSourceName("数据流图1.xml");
        response.setSourceType("POWERDESIGNER_DFD");
        response.setProcesses(List.of(process("o20", "生成消息")));
        response.setComponentCandidates(List.of(candidate("CAND_EI_o9", "EI")));

        when(fpParseService.parse(any(FpParseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/metrics/fp/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FpParseRequest(
                                "数据流图1",
                                "数据流图1.xml",
                                "POWERDESIGNER_DFD",
                                "<xml/>"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("FP"))
                .andExpect(jsonPath("$.contractKind").value("PARSE_PREVIEW"))
                .andExpect(jsonPath("$.processes[0].processName").value("生成消息"))
                .andExpect(jsonPath("$.componentCandidates[0].componentType").value("EI"));
    }

    @Test
    void shouldRejectBlankXmlContent() throws Exception {
        mockMvc.perform(post("/api/metrics/fp/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FpParseRequest(
                                "数据流图1",
                                "数据流图1.xml",
                                "POWERDESIGNER_DFD",
                                ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeFpCalculateEndpoint() throws Exception {
        FpCalculateResponse response = new FpCalculateResponse();
        response.setModule("FP");
        response.setContractKind("CALCULATION");
        response.setUfp(new java.math.BigDecimal("17.0000"));
        response.setVaf(new java.math.BigDecimal("0.8800"));
        response.setFp(new java.math.BigDecimal("14.9600"));
        response.setLocEstimate(898);

        when(fpCalculateService.calculate(any(FpCalculateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/metrics/fp/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FpCalculateRequest(
                                "数据流图1",
                                List.of(new FpCalculateRequest.ComponentInput("c1", "ILF", "消息", 20, 2, null, null)),
                                List.of(),
                                new FpCalculateRequest.LanguageInput("JAVA", 60)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("FP"))
                .andExpect(jsonPath("$.contractKind").value("CALCULATION"))
                .andExpect(jsonPath("$.ufp").value(17.0))
                .andExpect(jsonPath("$.vaf").value(0.88))
                .andExpect(jsonPath("$.fp").value(14.96))
                .andExpect(jsonPath("$.locEstimate").value(898));
    }

    private FpParseResponse.DfdProcessPreview process(String processId, String processName) {
        FpParseResponse.DfdProcessPreview process = new FpParseResponse.DfdProcessPreview();
        process.setProcessId(processId);
        process.setProcessName(processName);
        return process;
    }

    private FpParseResponse.ComponentCandidate candidate(String candidateId, String componentType) {
        FpParseResponse.ComponentCandidate candidate = new FpParseResponse.ComponentCandidate();
        candidate.setCandidateId(candidateId);
        candidate.setComponentType(componentType);
        candidate.setName("测试候选");
        return candidate;
    }
}
