package ca.cornalix.scoring.score;

import ca.cornalix.scoring.client.AnswerSummary;
import ca.cornalix.scoring.client.DiagnosticClient;
import ca.cornalix.scoring.client.QuestionSummary;
import ca.cornalix.scoring.score.dto.ScoreResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SCRUM-14 : score par fonction NIST CSF 2.0 = moyenne des reponses
 * REPONDUES dans cette fonction (YES=1, PARTIAL=0.5, NO=0,
 * NOT_APPLICABLE exclu). Les questions non repondues sont exclues, pas
 * comptees comme 0 -- un questionnaire partiellement rempli affiche un
 * score sur ce qui a ete repondu, pas une penalite sur ce qui manque.
 *
 * Recalcule a chaque appel, rien n'est mis en cache ni stocke (voir hors
 * scope de SCRUM-14).
 */
@Service
public class ScoreService {

    private final DiagnosticClient diagnosticClient;

    public ScoreService(DiagnosticClient diagnosticClient) {
        this.diagnosticClient = diagnosticClient;
    }

    public ScoreResponse computeScore(UUID organizationId, String authorizationHeader) {
        List<QuestionSummary> questions = diagnosticClient.fetchQuestions(authorizationHeader);
        List<AnswerSummary> answers = diagnosticClient.fetchAnswers(authorizationHeader, organizationId);

        Map<UUID, String> nistFunctionByQuestionId = new LinkedHashMap<>();
        questions.forEach(q -> nistFunctionByQuestionId.put(q.id(), q.nistFunction()));

        Map<String, List<Double>> pointsByFunction = new LinkedHashMap<>();
        for (AnswerSummary answer : answers) {
            String nistFunction = nistFunctionByQuestionId.get(answer.questionId());
            if (nistFunction == null) {
                continue; // reponse a une question qui n'existe plus dans le catalogue
            }
            Double points = pointsFor(answer.value());
            if (points == null) {
                continue; // NOT_APPLICABLE (ou valeur inconnue), exclu du calcul
            }
            pointsByFunction.computeIfAbsent(nistFunction, key -> new ArrayList<>()).add(points);
        }

        Map<String, Double> scoreByFunction = new LinkedHashMap<>();
        pointsByFunction.forEach((function, points) -> scoreByFunction.put(function, average(points)));

        Double overallScore = scoreByFunction.isEmpty() ? null : average(new ArrayList<>(scoreByFunction.values()));

        return new ScoreResponse(organizationId, overallScore, scoreByFunction);
    }

    private Double pointsFor(String value) {
        return switch (value) {
            case "YES" -> 1.0;
            case "PARTIAL" -> 0.5;
            case "NO" -> 0.0;
            default -> null; // NOT_APPLICABLE, ou valeur future non reconnue
        };
    }

    private double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
