package ca.cornalix.scoring.history;

import ca.cornalix.scoring.history.dto.ScoreSnapshotResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Historique des scores calcules (SCRUM-47) : au plus un point par
 * organisation et par jour civil, mis a jour -- jamais duplique -- a
 * chaque fois qu'un score est effectivement calcule (ScoreService). Pas
 * de tache planifiee : le point du jour n'existe que si quelqu'un a
 * consulte le score ce jour-la (dashboard, rapport, portefeuille), ce
 * qui est juge suffisant pour une tendance mensuelle/trimestrielle.
 */
@Service
public class ScoreHistoryService {

    private final ScoreSnapshotRepository repository;

    public ScoreHistoryService(ScoreSnapshotRepository repository) {
        this.repository = repository;
    }

    public void recordSnapshot(UUID organizationId, Double overallScore) {
        LocalDate today = LocalDate.now();

        repository.findByOrganizationIdAndSnapshotDate(organizationId, today)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(overallScore);
                            repository.save(existing);
                        },
                        () -> repository.save(new ScoreSnapshot(organizationId, overallScore, today)));
    }

    public List<ScoreSnapshotResponse> getHistory(UUID organizationId) {
        return repository.findAllByOrganizationIdOrderBySnapshotDateAsc(organizationId).stream()
                .map(s -> new ScoreSnapshotResponse(s.getSnapshotDate(), s.getOverallScore()))
                .toList();
    }
}
