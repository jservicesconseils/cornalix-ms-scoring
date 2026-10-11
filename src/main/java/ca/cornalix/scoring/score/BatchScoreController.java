package ca.cornalix.scoring.score;

import ca.cornalix.scoring.score.dto.BatchScoreRequest;
import ca.cornalix.scoring.score.dto.ScoreResponse;
import ca.cornalix.scoring.security.TenantClaims;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Score de plusieurs organisations en un seul appel (SCRUM-43) --
 * remplace le pattern N+1 du Portefeuille Consultant (un GET par
 * organisation cote navigateur). Chaque organisation demandee doit
 * etre dans le tenant_scope de l'appelant, verifiee une a une avant
 * tout calcul -- aucune ne doit pouvoir "passer" dans la liste sans
 * acces, meme si d'autres organisations demandees sont legitimes.
 */
@RestController
@RequestMapping("/api/v1/scoring/organizations/batch-scores")
public class BatchScoreController {

    private final ScoreService service;

    public BatchScoreController(ScoreService service) {
        this.service = service;
    }

    @PostMapping
    public Map<UUID, ScoreResponse> getBatchScores(
            @Valid @RequestBody BatchScoreRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantClaims claims = TenantClaims.from(jwt);
        request.organizationIds().forEach(claims::assertAccessTo);

        return service.computeScores(request.organizationIds(), "Bearer " + jwt.getTokenValue());
    }
}
