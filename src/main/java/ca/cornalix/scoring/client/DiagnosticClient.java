package ca.cornalix.scoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * Appelle cornalix-ms-diagnostic en HTTP (SCRUM-7/8/9 : microservices
 * separes des le MVP, communication HTTP simple en Phase 1 -- pas encore
 * de bus d'evenements).
 *
 * Le jeton de l'appelant (en-tete Authorization complet, "Bearer ...") est
 * transmis tel quel : c'est le meme utilisateur qui interroge son propre
 * score, donc la verification tenant_id/tenant_scope de cornalix-ms-
 * diagnostic (SCRUM-12) s'applique normalement, sans mecanisme de
 * confiance service-a-service separe pour cette premiere version.
 */
@Component
public class DiagnosticClient {

    private final RestClient restClient;

    public DiagnosticClient(RestClient.Builder builder, @Value("${cornalix.diagnostic.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<QuestionSummary> fetchQuestions(String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/diagnostics/questions")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<QuestionSummary>>() { });
        } catch (RestClientException e) {
            throw new DiagnosticUnavailableException(
                    "Impossible de recuperer le catalogue de questions depuis cornalix-ms-diagnostic", e);
        }
    }

    public List<AnswerSummary> fetchAnswers(String authorizationHeader, UUID organizationId) {
        try {
            return restClient.get()
                    .uri("/api/v1/diagnostics/organizations/{organizationId}/answers", organizationId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AnswerSummary>>() { });
        } catch (RestClientException e) {
            throw new DiagnosticUnavailableException(
                    "Impossible de recuperer les reponses depuis cornalix-ms-diagnostic pour l'organisation " + organizationId, e);
        }
    }
}
