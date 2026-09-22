package ca.cornalix.scoring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantClaimsTest {

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void tenantIdSeul_accesUniquementAuPropreTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID autreTenant = UUID.randomUUID();
        Jwt jwt = jwtWithClaims(Map.of("tenant_id", tenantId.toString()));

        TenantClaims claims = TenantClaims.from(jwt);

        assertEquals(tenantId, claims.tenantId());
        assertTrue(claims.hasAccessTo(tenantId));
        assertFalse(claims.hasAccessTo(autreTenant));
    }

    @Test
    void tenantScopeMultiTenant_accesATousLesTenantsListes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID tenantC = UUID.randomUUID();
        String tenantScopeJson = "[\"" + tenantA + "\",\"" + tenantB + "\"]";
        Jwt jwt = jwtWithClaims(Map.of("tenant_id", tenantA.toString(), "tenant_scope", tenantScopeJson));

        TenantClaims claims = TenantClaims.from(jwt);

        assertTrue(claims.hasAccessTo(tenantA));
        assertTrue(claims.hasAccessTo(tenantB));
        assertFalse(claims.hasAccessTo(tenantC));
    }

    @Test
    void aucunTenantId_aucunAcces() {
        Jwt jwt = jwtWithClaims(Map.of());

        TenantClaims claims = TenantClaims.from(jwt);

        assertNull(claims.tenantId());
        assertFalse(claims.hasAccessTo(UUID.randomUUID()));
    }

    @Test
    void tenantIdInvalide_traiteCommeAbsent() {
        Jwt jwt = jwtWithClaims(Map.of("tenant_id", "pas-un-uuid"));

        TenantClaims claims = TenantClaims.from(jwt);

        assertNull(claims.tenantId());
        assertFalse(claims.hasAccessTo(UUID.randomUUID()));
    }

    @Test
    void tenantScopeIllisible_retombeSurLeSeulTenantId() {
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = jwtWithClaims(Map.of("tenant_id", tenantId.toString(), "tenant_scope", "{pas du json valide"));

        TenantClaims claims = TenantClaims.from(jwt);

        assertTrue(claims.hasAccessTo(tenantId));
        assertEquals(1, claims.tenantScope().size());
    }

    @Test
    void assertAccessTo_leveTenantAccessDeniedExceptionSiHorsScope() {
        UUID tenantId = UUID.randomUUID();
        UUID autreTenant = UUID.randomUUID();
        Jwt jwt = jwtWithClaims(Map.of("tenant_id", tenantId.toString()));

        TenantClaims claims = TenantClaims.from(jwt);

        assertThrows(TenantAccessDeniedException.class, () -> claims.assertAccessTo(autreTenant));
    }
}
