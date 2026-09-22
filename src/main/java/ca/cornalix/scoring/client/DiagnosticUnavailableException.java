package ca.cornalix.scoring.client;

/**
 * Levee quand l'appel HTTP vers cornalix-ms-diagnostic echoue (service
 * injoignable, erreur inattendue). Traduite en reponse HTTP 502 (Bad
 * Gateway) -- ce service agit comme passerelle vers cornalix-ms-diagnostic
 * pour calculer un score, une panne en amont n'est pas une erreur du
 * client appelant.
 */
public class DiagnosticUnavailableException extends RuntimeException {

    public DiagnosticUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
