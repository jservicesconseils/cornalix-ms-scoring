package ca.cornalix.scoring.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class DiagnosticExceptionHandler {

    @ExceptionHandler(DiagnosticUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleDiagnosticUnavailable(DiagnosticUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(ex.getMessage()));
    }

    // SCRUM-49 : le circuit breaker rejette l'appel sans meme invoquer
    // DiagnosticClient (echecs recents trop nombreux) -- meme reponse 502
    // que DiagnosticUnavailableException, le client appelant n'a pas a
    // distinguer "diagnostic a echoue maintenant" de "diagnostic marque
    // en panne, on evite de le marteler".
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(CallNotPermittedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorBody(
                "cornalix-ms-diagnostic est temporairement marque indisponible (circuit ouvert), reessayez dans quelques instants"));
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("message", message);
        return body;
    }
}
