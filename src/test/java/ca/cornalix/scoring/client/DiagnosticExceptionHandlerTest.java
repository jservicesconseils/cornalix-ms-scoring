package ca.cornalix.scoring.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticExceptionHandlerTest {

    private final DiagnosticExceptionHandler handler = new DiagnosticExceptionHandler();

    @Test
    void diagnosticIndisponible_renvoie502() {
        DiagnosticUnavailableException ex = new DiagnosticUnavailableException("message de test", new RuntimeException());

        ResponseEntity<Map<String, Object>> response = handler.handleDiagnosticUnavailable(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("message de test", response.getBody().get("message"));
    }

    // SCRUM-49 : quand le circuit est ouvert (echecs recents trop
    // nombreux), Resilience4j rejette l'appel avant meme d'invoquer
    // DiagnosticClient -- meme reponse 502 que DiagnosticUnavailableException.
    @Test
    void circuitOuvert_renvoie502() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("diagnosticClient");
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        ResponseEntity<Map<String, Object>> response = handler.handleCircuitOpen(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("circuit ouvert"));
    }
}
