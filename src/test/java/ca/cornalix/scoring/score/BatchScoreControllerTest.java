package ca.cornalix.scoring.score;

import ca.cornalix.scoring.score.dto.BatchScoreRequest;
import ca.cornalix.scoring.score.dto.ScoreResponse;
import ca.cornalix.scoring.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchScoreController.class)
@Import(SecurityConfig.class)
class BatchScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScoreService scoreService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void obtenirLesScoresEnLot_organisationsToutesDansLeScope_renvoie200() throws Exception {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        String tenantScopeJson = "[\"" + orgA + "\",\"" + orgB + "\"]";

        when(scoreService.computeScores(eq(List.of(orgA, orgB)), anyString())).thenReturn(Map.of(
                orgA, new ScoreResponse(orgA, 0.8, Map.of(), Map.of()),
                orgB, new ScoreResponse(orgB, 0.4, Map.of(), Map.of())));

        mockMvc.perform(post("/api/v1/scoring/organizations/batch-scores")
                        .with(jwt().jwt(builder -> builder
                                .claim("tenant_id", orgA.toString())
                                .claim("tenant_scope", tenantScopeJson)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchScoreRequest(List.of(orgA, orgB)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['" + orgA + "'].overallScore").value(0.8))
                .andExpect(jsonPath("$['" + orgB + "'].overallScore").value(0.4));
    }

    @Test
    void obtenirLesScoresEnLot_uneOrganisationHorsScope_renvoie403() throws Exception {
        UUID orgDansLeScope = UUID.randomUUID();
        UUID orgHorsScope = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/scoring/organizations/batch-scores")
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", orgDansLeScope.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BatchScoreRequest(List.of(orgDansLeScope, orgHorsScope)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenirLesScoresEnLot_listeVide_renvoie400() throws Exception {
        mockMvc.perform(post("/api/v1/scoring/organizations/batch-scores")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchScoreRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenirLesScoresEnLot_sansJeton_renvoie401() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/scoring/organizations/batch-scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchScoreRequest(List.of(orgId)))))
                .andExpect(status().isUnauthorized());
    }
}
