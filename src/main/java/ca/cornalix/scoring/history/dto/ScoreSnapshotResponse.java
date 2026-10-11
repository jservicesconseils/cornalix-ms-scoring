package ca.cornalix.scoring.history.dto;

import java.time.LocalDate;

/** Un point de la courbe de tendance. */
public record ScoreSnapshotResponse(LocalDate date, Double overallScore) {
}
