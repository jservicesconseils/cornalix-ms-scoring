package ca.cornalix.scoring.history;

import ca.cornalix.scoring.history.dto.ScoreSnapshotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreHistoryServiceTest {

    @Mock
    private ScoreSnapshotRepository repository;

    private ScoreHistoryService service() {
        return new ScoreHistoryService(repository);
    }

    @Test
    void recordSnapshot_aucunPointAujourdhui_enCreeUnNouveau() {
        UUID orgId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndSnapshotDate(orgId, LocalDate.now())).thenReturn(Optional.empty());

        service().recordSnapshot(orgId, 0.75);

        ArgumentCaptor<ScoreSnapshot> saved = ArgumentCaptor.forClass(ScoreSnapshot.class);
        verify(repository).save(saved.capture());
        assertEquals(orgId, saved.getValue().getOrganizationId());
        assertEquals(0.75, saved.getValue().getOverallScore());
        assertEquals(LocalDate.now(), saved.getValue().getSnapshotDate());
    }

    @Test
    void recordSnapshot_pointDejaPresentAujourdhui_leMetAJourSansEnCreerUnSecond() {
        UUID orgId = UUID.randomUUID();
        ScoreSnapshot existing = new ScoreSnapshot(orgId, 0.4, LocalDate.now());
        when(repository.findByOrganizationIdAndSnapshotDate(orgId, LocalDate.now())).thenReturn(Optional.of(existing));

        service().recordSnapshot(orgId, 0.9);

        verify(repository).save(existing);
        assertEquals(0.9, existing.getOverallScore());
    }

    @Test
    void getHistory_renvoieLesPointsOrdonnesParDate() {
        UUID orgId = UUID.randomUUID();
        when(repository.findAllByOrganizationIdOrderBySnapshotDateAsc(orgId)).thenReturn(List.of(
                new ScoreSnapshot(orgId, 0.3, LocalDate.now().minusDays(1)),
                new ScoreSnapshot(orgId, 0.5, LocalDate.now())));

        List<ScoreSnapshotResponse> history = service().getHistory(orgId);

        assertEquals(2, history.size());
        assertEquals(0.3, history.get(0).overallScore());
        assertEquals(0.5, history.get(1).overallScore());
    }
}
