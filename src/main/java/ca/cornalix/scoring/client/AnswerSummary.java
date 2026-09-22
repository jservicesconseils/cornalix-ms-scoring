package ca.cornalix.scoring.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Vue minimale d'une reponse telle que renvoyee par
 * GET /api/v1/diagnostics/organizations/{id}/answers sur
 * cornalix-ms-diagnostic (SCRUM-12). {@code value} reste une String brute
 * ("YES"/"NO"/"PARTIAL"/"NOT_APPLICABLE") plutot qu'un enum local -- evite
 * de faire echouer le scoring si cornalix-ms-diagnostic ajoute une valeur
 * que ce service ne connait pas encore (voir ScoreService.pointsFor, qui
 * ignore silencieusement toute valeur non reconnue).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnswerSummary(UUID questionId, String value) {
}
