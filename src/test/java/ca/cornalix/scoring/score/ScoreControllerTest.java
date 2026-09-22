package ca.cornalix.scoring.score;

import ca.cornalix.scoring.client.DiagnosticUnavailableException;
import ca.cornalix.scoring.score.dto.ScoreResponse;
import ca.cornalix.scoring.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreController.class)
@Import(SecurityConfig.class)
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScoreService scoreService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void lireLeScore_avecTenantIdCorrespondant_renvoie200() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(scoreService.computeScore(eq(orgId), anyString()))
                .thenReturn(new ScoreResponse(orgId, 0.75, Map.of("IDENTIFY", 0.75), Map.of(1, 0.75)));

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score", orgId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", orgId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(0.75))
                .andExpect(jsonPath("$.scoreByFunction.IDENTIFY").value(0.75))
                .andExpect(jsonPath("$.scoreByCisControl.1").value(0.75));
    }

    @Test
    void lireLeScore_avecTenantIdDifferent_renvoie403() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID autreTenant = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score", orgId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", autreTenant.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lireLeScore_sansTenantId_renvoie403() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score", orgId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lireLeScore_sansJeton_renvoie401() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score", orgId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lireLeScore_diagnosticIndisponible_renvoie502() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(scoreService.computeScore(eq(orgId), anyString()))
                .thenThrow(new DiagnosticUnavailableException("indisponible", new RuntimeException()));

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score", orgId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", orgId.toString()))))
                .andExpect(status().isBadGateway());
    }
}
