package ca.cornalix.scoring.score.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Ce qu'un client envoie pour obtenir le score de plusieurs organisations en un seul appel (SCRUM-43). */
public record BatchScoreRequest(

        @NotEmpty(message = "la liste d'organisations est requise")
        List<UUID> organizationIds
) {
}
