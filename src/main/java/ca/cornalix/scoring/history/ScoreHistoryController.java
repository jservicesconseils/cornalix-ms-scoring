package ca.cornalix.scoring.history;

import ca.cornalix.scoring.history.dto.ScoreSnapshotResponse;
import ca.cornalix.scoring.security.TenantClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Meme garde tenant que ScoreController (SCRUM-47). */
@RestController
@RequestMapping("/api/v1/scoring/organizations/{organizationId}/score/history")
public class ScoreHistoryController {

    private final ScoreHistoryService service;

    public ScoreHistoryController(ScoreHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScoreSnapshotResponse> getHistory(@PathVariable UUID organizationId, @AuthenticationPrincipal Jwt jwt) {
        TenantClaims.from(jwt).assertAccessTo(organizationId);
        return service.getHistory(organizationId);
    }
}
