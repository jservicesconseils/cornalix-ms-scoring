package ca.cornalix.scoring.score.dto;

import java.util.Map;
import java.util.UUID;

/**
 * {@code overallScore} et les entrees de {@code scoreByFunction}/
 * {@code scoreByCisControl} sont {@code null}/vides quand l'organisation
 * n'a encore repondu a aucune question exploitable (aucune reponse, ou
 * uniquement des NOT_APPLICABLE) -- distinct d'un score de 0.0, qui
 * signifierait "tout est a NO".
 *
 * Deux vues du meme calcul (SCRUM-15) : {@code scoreByFunction} agrege par
 * fonction NIST CSF 2.0 (vue direction/board), {@code scoreByCisControl}
 * agrege par controle CIS Controls v8 (vue operationnelle, Responsable TI).
 */
public record ScoreResponse(
        UUID organizationId,
        Double overallScore,
        Map<String, Double> scoreByFunction,
        Map<Integer, Double> scoreByCisControl
) {
}
