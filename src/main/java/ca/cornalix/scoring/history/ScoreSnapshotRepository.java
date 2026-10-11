package ca.cornalix.scoring.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, UUID> {

    Optional<ScoreSnapshot> findByOrganizationIdAndSnapshotDate(UUID organizationId, LocalDate snapshotDate);

    List<ScoreSnapshot> findAllByOrganizationIdOrderBySnapshotDateAsc(UUID organizationId);
}
