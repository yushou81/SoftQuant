package com.softquant.backend.metrics.ucp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softquant.backend.metrics.ucp.dto.UcpParseRequest;
import com.softquant.backend.metrics.ucp.dto.UcpParseResponse;
import com.softquant.backend.metrics.ucp.service.UcpParseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UcpMetricsController.class)
class UcpMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UcpParseService ucpParseService;

    @Test
    void shouldExposeUcpParseEndpoint() throws Exception {
        UcpParseResponse response = new UcpParseResponse();
        response.setModule("UCP");
        response.setContractKind("PARSE_PREVIEW");
        response.setProjectName("在线教学系统");
        response.setSourceName("在线教学系统.xml");
        response.setSourceType("POWERDESIGNER_USE_CASE");
        response.setActors(List.of(actor("o81", "系统管理员")));
        response.setUseCases(List.of(useCase("o84", "系统登录")));
        response.setRelationships(List.of(relationship("o13", "ACTOR_ASSOCIATION")));
        response.setPendingFields(List.of(pendingField("entityCount", "o84")));
        response.setEvidence(List.of(evidence("UC_o84")));

        when(ucpParseService.parse(any(UcpParseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/metrics/ucp/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UcpParseRequest(
                                "在线教学系统",
                                "在线教学系统.xml",
                                "POWERDESIGNER_USE_CASE",
                                "<xml/>"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("UCP"))
                .andExpect(jsonPath("$.contractKind").value("PARSE_PREVIEW"))
                .andExpect(jsonPath("$.actors[0].actorName").value("系统管理员"))
                .andExpect(jsonPath("$.useCases[0].useCaseName").value("系统登录"))
                .andExpect(jsonPath("$.relationships[0].relationshipType").value("ACTOR_ASSOCIATION"))
                .andExpect(jsonPath("$.pendingFields[0].fieldKey").value("entityCount"))
                .andExpect(jsonPath("$.evidence[0].code").value("UC_o84"));
    }

    @Test
    void shouldRejectBlankXmlContent() throws Exception {
        mockMvc.perform(post("/api/metrics/ucp/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UcpParseRequest(
                                "在线教学系统",
                                "在线教学系统.xml",
                                "POWERDESIGNER_USE_CASE",
                                ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    private UcpParseResponse.UcpActorPreview actor(String actorId, String actorName) {
        UcpParseResponse.UcpActorPreview actor = new UcpParseResponse.UcpActorPreview();
        actor.setActorId(actorId);
        actor.setActorName(actorName);
        actor.setSuggestedComplexity("COMPLEX");
        actor.setSuggestedWeight(3);
        return actor;
    }

    private UcpParseResponse.UcpUseCasePreview useCase(String useCaseId, String useCaseName) {
        UcpParseResponse.UcpUseCasePreview useCase = new UcpParseResponse.UcpUseCasePreview();
        useCase.setUseCaseId(useCaseId);
        useCase.setUseCaseName(useCaseName);
        useCase.setSuggestedComplexity("AVERAGE");
        useCase.setSuggestedWeight(10);
        return useCase;
    }

    private UcpParseResponse.UcpRelationshipPreview relationship(String relationshipId, String relationshipType) {
        UcpParseResponse.UcpRelationshipPreview relationship = new UcpParseResponse.UcpRelationshipPreview();
        relationship.setRelationshipId(relationshipId);
        relationship.setRelationshipType(relationshipType);
        return relationship;
    }

    private UcpParseResponse.UcpPendingField pendingField(String fieldKey, String entityId) {
        UcpParseResponse.UcpPendingField pendingField = new UcpParseResponse.UcpPendingField();
        pendingField.setFieldKey(fieldKey);
        pendingField.setEntityId(entityId);
        pendingField.setReason("需要人工补录");
        return pendingField;
    }

    private UcpParseResponse.UcpEvidence evidence(String code) {
        UcpParseResponse.UcpEvidence evidence = new UcpParseResponse.UcpEvidence();
        evidence.setCode(code);
        evidence.setEvidenceType("RULE_HINT");
        evidence.setSummary("测试证据");
        return evidence;
    }
}
