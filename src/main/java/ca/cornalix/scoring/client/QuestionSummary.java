package ca.cornalix.scoring.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Vue minimale d'une question telle que renvoyee par
 * GET /api/v1/diagnostics/questions sur cornalix-ms-diagnostic (SCRUM-10/11)
 * -- seuls les champs utiles au calcul du score sont repris ici. Les autres
 * champs de la reponse (cisControl, cisSafeguard, translations, ...) sont
 * ignores plutot que de faire echouer la deserialisation (contrat HTTP
 * tolerant aux champs supplementaires).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionSummary(UUID id, String nistFunction) {
}
