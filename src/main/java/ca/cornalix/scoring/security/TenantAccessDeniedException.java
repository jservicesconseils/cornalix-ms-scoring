package ca.cornalix.scoring.security;

import java.util.UUID;

/**
 * Levee quand l'appelant n'a pas acces a l'organisation demandee (son
 * tenant_id/tenant_scope ne la couvre pas). Traduite en reponse HTTP 403
 * (Forbidden) par SecurityExceptionHandler.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(UUID organizationId) {
        super("Acces refuse : l'organisation " + organizationId + " n'est pas dans le tenant scope de l'appelant");
    }
}
