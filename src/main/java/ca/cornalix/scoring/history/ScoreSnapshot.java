package ca.cornalix.scoring.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un point de la courbe de tendance d'une organisation (SCRUM-47) --
 * au plus un par jour civil, voir ScoreHistoryService. overallScore
 * reste nullable (meme convention que ScoreResponse) : aucune reponse
 * exploitable ce jour-la n'est distinct d'un score de 0.0.
 *
 * Seul overallScore est conserve ici, pas le detail par fonction NIST/
 * controle CIS -- c'est la seule courbe demandee (tendance globale) ;
 * le detail reste disponible via /score pour le jour courant.
 */
@Entity
@Table(name = "score_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "snapshot_date"})
})
public class ScoreSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScoreSnapshot() {
        // constructeur requis par JPA, ne pas utiliser directement
    }

    public ScoreSnapshot(UUID organizationId, Double overallScore, LocalDate snapshotDate) {
        this.organizationId = organizationId;
        this.overallScore = overallScore;
        this.snapshotDate = snapshotDate;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Reecrit le point du jour plutot que d'en creer un second (SCRUM-47
    // : au plus un par jour civil) -- un consultant qui regarde le
    // Portefeuille plusieurs fois dans la journee ne doit jamais faire
    // apparaitre plusieurs points pour la meme date.
    public void update(Double overallScore) {
        this.overallScore = overallScore;
        this.updatedAt = Instant.now();
    }
}
