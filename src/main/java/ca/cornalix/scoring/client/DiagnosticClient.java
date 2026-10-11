package ca.cornalix.scoring.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
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
 *
 * SCRUM-49 : timeout explicite (sans ca, un diagnostic qui ne repond plus
 * du tout bloquerait l'appelant indefiniment -- les defauts du JDK sont
 * illimites) + retry avec backoff et circuit breaker Resilience4j (config
 * resilience4j.* dans application.yml, instance "diagnosticClient") pour
 * qu'une panne/lenteur transitoire en amont ne se traduise pas
 * immediatement en echec et ne martele pas un service deja en panne.
 * Les deux annotations ne voient jamais RestClientException directement
 * (catchee plus bas) : elles agissent sur DiagnosticUnavailableException,
 * seule exception qui traverse la frontiere de la methode.
 */
@Component
public class DiagnosticClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public DiagnosticClient(RestClient.Builder builder, @Value("${cornalix.diagnostic.base-url}") String baseUrl) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(CONNECT_TIMEOUT)
                        .withReadTimeout(READ_TIMEOUT));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Retry(name = "diagnosticClient")
    @CircuitBreaker(name = "diagnosticClient")
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

    @Retry(name = "diagnosticClient")
    @CircuitBreaker(name = "diagnosticClient")
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
