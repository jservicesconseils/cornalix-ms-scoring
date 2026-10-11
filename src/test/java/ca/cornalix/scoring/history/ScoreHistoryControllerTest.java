package ca.cornalix.scoring.history;

import ca.cornalix.scoring.history.dto.ScoreSnapshotResponse;
import ca.cornalix.scoring.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreHistoryController.class)
@Import(SecurityConfig.class)
class ScoreHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScoreHistoryService scoreHistoryService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void getHistory_avecTenantIdCorrespondant_renvoie200() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(scoreHistoryService.getHistory(orgId)).thenReturn(List.of(
                new ScoreSnapshotResponse(LocalDate.now().minusDays(1), 0.4),
                new ScoreSnapshotResponse(LocalDate.now(), 0.6)));

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score/history", orgId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", orgId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].overallScore").value(0.6));
    }

    @Test
    void getHistory_avecTenantIdDifferent_renvoie403() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID autreTenant = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score/history", orgId)
                        .with(jwt().jwt(builder -> builder.claim("tenant_id", autreTenant.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHistory_sansJeton_renvoie401() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/scoring/organizations/{orgId}/score/history", orgId))
                .andExpect(status().isUnauthorized());
    }
}
