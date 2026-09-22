package ca.cornalix.scoring.score.dto;

import java.util.Map;
import java.util.UUID;

/**
 * {@code overallScore} et les entrees de {@code scoreByFunction} sont
 * {@code null} quand l'organisation n'a encore repondu a aucune question
 * exploitable (aucune reponse, ou uniquement des NOT_APPLICABLE) --
 * distinct d'un score de 0.0, qui signifierait "tout est a NO".
 */
public record ScoreResponse(
        UUID organizationId,
        Double overallScore,
        Map<String, Double> scoreByFunction
) {
}
