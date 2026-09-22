package ca.cornalix.scoring.score;

import ca.cornalix.scoring.score.dto.ScoreResponse;
import ca.cornalix.scoring.security.TenantClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Meme garde-fou tenant que cornalix-ms-identity (SCRUM-6) et
 * cornalix-ms-diagnostic (SCRUM-12) : le tenant_id/tenant_scope de
 * l'appelant est verifie contre {organizationId} avant tout calcul.
 *
 * Le jeton brut a retransmettre a cornalix-ms-diagnostic est reconstruit
 * depuis le Jwt deja resolu ("Bearer " + jwt.getTokenValue()) plutot que
 * lu via un @RequestHeader separe -- evite de faire retraiter l'en-tete
 * Authorization une seconde fois par le filtre de securite.
 */
@RestController
@RequestMapping("/api/v1/scoring/organizations/{organizationId}/score")
public class ScoreController {

    private final ScoreService service;

    public ScoreController(ScoreService service) {
        this.service = service;
    }

    @GetMapping
    public ScoreResponse getScore(@PathVariable UUID organizationId, @AuthenticationPrincipal Jwt jwt) {
        TenantClaims.from(jwt).assertAccessTo(organizationId);
        return service.computeScore(organizationId, "Bearer " + jwt.getTokenValue());
    }
}
