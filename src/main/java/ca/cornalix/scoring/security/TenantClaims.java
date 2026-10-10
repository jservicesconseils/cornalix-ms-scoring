package ca.cornalix.scoring.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

/**
 * Les claims de tenant portes par un jeton Cognito (voir SCRUM-3, trigger
 * Lambda "Pre Token Generation" sur cornalix-ms-identity) : le tenant
 * "principal" de l'appelant, et l'ensemble des tenants qu'il a le droit de
 * consulter (un seul pour la plupart des roles, plusieurs pour un
 * Consultant cybersecurite).
 *
 * Port direct de la classe du meme nom sur cornalix-ms-identity (SCRUM-6)
 * et cornalix-ms-diagnostic (SCRUM-12). Duplique plutot que partage via une
 * librairie commune -- voir la decision de garder des microservices
 * independants des le MVP (SCRUM-7/8/9).
 *
 * Absence de {@code tenant_id} sur le jeton se traduit ici par un acces
 * refuse a toute organisation -- le login n'est jamais bloque (voir le
 * comportement "fail-open" du trigger Lambda), mais un jeton sans tenant
 * ne franchit aucune route scopee par tenant.
 */
public record TenantClaims(UUID tenantId, List<UUID> tenantScope) {

    private static final Logger log = LoggerFactory.getLogger(TenantClaims.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TenantClaims from(Jwt jwt) {
        UUID tenantId = parseTenantId(jwt.getClaimAsString("tenant_id"), jwt.getSubject());
        // tenantScope est lu meme sans tenantId (SCRUM-45) : un Consultant
        // cybersecurite n'a jamais de tenant_id principal, seulement un
        // claim tenant_scope porte par le portefeuille d'organisations qui
        // lui ont donne acces.
        return new TenantClaims(tenantId, parseTenantScope(jwt.getClaimAsString("tenant_scope"), tenantId));
    }

    public boolean hasAccessTo(UUID organizationId) {
        return tenantScope.contains(organizationId);
    }

    public void assertAccessTo(UUID organizationId) {
        if (!hasAccessTo(organizationId)) {
            throw new TenantAccessDeniedException(organizationId);
        }
    }

    private static UUID parseTenantId(String rawTenantId, String subject) {
        if (rawTenantId == null || rawTenantId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawTenantId);
        } catch (IllegalArgumentException e) {
            log.warn("Claim tenant_id invalide (pas un UUID) sur le jeton de {} : {}", subject, rawTenantId);
            return null;
        }
    }

    // Sur une claim tenant_scope illisible ou absente, on retombe sur
    // [tenantId] si un tenantId existe (l'appelant garde acces a son
    // propre tenant, jamais plus) -- ou sur une liste vide sinon
    // (Consultant sans tenant_id et sans portefeuille encore accorde,
    // SCRUM-45 : aucun repli possible, aucun acces).
    private static List<UUID> parseTenantScope(String rawTenantScope, UUID tenantId) {
        List<UUID> fallback = tenantId == null ? List.of() : List.of(tenantId);

        if (rawTenantScope == null || rawTenantScope.isBlank()) {
            return fallback;
        }

        try {
            List<String> ids = OBJECT_MAPPER.readValue(rawTenantScope, new TypeReference<List<String>>() { });
            List<UUID> tenantScope = ids.stream().map(UUID::fromString).toList();
            return tenantScope.isEmpty() ? fallback : tenantScope;
        } catch (Exception e) {
            log.warn("Claim tenant_scope illisible pour le tenant {}, acces restreint a ce seul tenant : {}", tenantId, rawTenantScope);
            return fallback;
        }
    }
}
