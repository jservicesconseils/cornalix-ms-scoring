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
 * SCRUM-14/SCRUM-15 : score = moyenne des reponses REPONDUES dans un
 * groupe (YES=1, PARTIAL=0.5, NO=0, NOT_APPLICABLE exclu), agrege deux
 * fois sur les memes reponses : par fonction NIST CSF 2.0
 * ({@code scoreByFunction}, vue direction) et par controle CIS Controls
 * v8 ({@code scoreByCisControl}, vue operationnelle). Les questions non
 * repondues sont exclues, pas comptees comme 0 -- un questionnaire
 * partiellement rempli affiche un score sur ce qui a ete repondu, pas une
 * penalite sur ce qui manque.
 *
 * Recalcule a chaque appel, rien n'est mis en cache ni stocke.
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
        Map<UUID, Integer> cisControlByQuestionId = new LinkedHashMap<>();
        questions.forEach(q -> {
            nistFunctionByQuestionId.put(q.id(), q.nistFunction());
            cisControlByQuestionId.put(q.id(), q.cisControl());
        });

        Map<String, Double> scoreByFunction = scoreBy(answers, nistFunctionByQuestionId);
        Map<Integer, Double> scoreByCisControl = scoreBy(answers, cisControlByQuestionId);

        Double overallScore = scoreByFunction.isEmpty() ? null : average(new ArrayList<>(scoreByFunction.values()));

        return new ScoreResponse(organizationId, overallScore, scoreByFunction, scoreByCisControl);
    }

    /** Agrege les reponses par la cle fournie (fonction NIST, ou controle CIS) -- meme logique, deux groupements. */
    private <K> Map<K, Double> scoreBy(List<AnswerSummary> answers, Map<UUID, K> keyByQuestionId) {
        Map<K, List<Double>> pointsByKey = new LinkedHashMap<>();

        for (AnswerSummary answer : answers) {
            K key = keyByQuestionId.get(answer.questionId());
            if (key == null) {
                continue; // reponse a une question qui n'existe plus dans le catalogue
            }
            Double points = pointsFor(answer.value());
            if (points == null) {
                continue; // NOT_APPLICABLE (ou valeur inconnue), exclu du calcul
            }
            pointsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(points);
        }

        Map<K, Double> scoreByKey = new LinkedHashMap<>();
        pointsByKey.forEach((key, points) -> scoreByKey.put(key, average(points)));
        return scoreByKey;
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
