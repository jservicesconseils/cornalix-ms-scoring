package ca.cornalix.scoring.score;

import ca.cornalix.scoring.client.AnswerSummary;
import ca.cornalix.scoring.client.DiagnosticClient;
import ca.cornalix.scoring.client.QuestionSummary;
import ca.cornalix.scoring.score.dto.ScoreResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private DiagnosticClient diagnosticClient;

    private ScoreService service() {
        return new ScoreService(diagnosticClient);
    }

    @Test
    void computeScore_reponsesMixtes_calculeLaMoyenneParFonctionEnExcluantNotApplicable() {
        ScoreService service = service();
        UUID orgId = UUID.randomUUID();
        UUID q1 = UUID.randomUUID(); // IDENTIFY, YES
        UUID q2 = UUID.randomUUID(); // IDENTIFY, NO
        UUID q3 = UUID.randomUUID(); // PROTECT, PARTIAL
        UUID q4 = UUID.randomUUID(); // PROTECT, NOT_APPLICABLE -- exclu

        when(diagnosticClient.fetchQuestions("Bearer token")).thenReturn(List.of(
                new QuestionSummary(q1, "IDENTIFY"),
                new QuestionSummary(q2, "IDENTIFY"),
                new QuestionSummary(q3, "PROTECT"),
                new QuestionSummary(q4, "PROTECT")
        ));
        when(diagnosticClient.fetchAnswers("Bearer token", orgId)).thenReturn(List.of(
                new AnswerSummary(q1, "YES"),
                new AnswerSummary(q2, "NO"),
                new AnswerSummary(q3, "PARTIAL"),
                new AnswerSummary(q4, "NOT_APPLICABLE")
        ));

        ScoreResponse response = service.computeScore(orgId, "Bearer token");

        assertEquals(0.5, response.scoreByFunction().get("IDENTIFY")); // (1 + 0) / 2
        assertEquals(0.5, response.scoreByFunction().get("PROTECT"));  // seul PARTIAL compte (q4 exclue)
        assertEquals(0.5, response.overallScore()); // moyenne des 2 fonctions, toutes deux a 0.5
    }

    @Test
    void computeScore_questionsNonRepondues_sontExcluesPasComptéesCommeZero() {
        ScoreService service = service();
        UUID orgId = UUID.randomUUID();
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID(); // jamais repondue

        when(diagnosticClient.fetchQuestions("Bearer token")).thenReturn(List.of(
                new QuestionSummary(q1, "IDENTIFY"),
                new QuestionSummary(q2, "IDENTIFY")
        ));
        when(diagnosticClient.fetchAnswers("Bearer token", orgId)).thenReturn(List.of(
                new AnswerSummary(q1, "YES")
        ));

        ScoreResponse response = service.computeScore(orgId, "Bearer token");

        assertEquals(1.0, response.scoreByFunction().get("IDENTIFY"));
    }

    @Test
    void computeScore_aucuneReponseExploitable_renvoieDesScoresNulsPasZero() {
        ScoreService service = service();
        UUID orgId = UUID.randomUUID();

        when(diagnosticClient.fetchQuestions("Bearer token")).thenReturn(List.of());
        when(diagnosticClient.fetchAnswers("Bearer token", orgId)).thenReturn(List.of());

        ScoreResponse response = service.computeScore(orgId, "Bearer token");

        assertNull(response.overallScore());
        assertTrue(response.scoreByFunction().isEmpty());
    }

    @Test
    void computeScore_reponseAUneQuestionInconnue_estIgnoree() {
        ScoreService service = service();
        UUID orgId = UUID.randomUUID();
        UUID questionSupprimee = UUID.randomUUID();

        when(diagnosticClient.fetchQuestions("Bearer token")).thenReturn(List.of());
        when(diagnosticClient.fetchAnswers("Bearer token", orgId)).thenReturn(List.of(
                new AnswerSummary(questionSupprimee, "YES")
        ));

        ScoreResponse response = service.computeScore(orgId, "Bearer token");

        assertNull(response.overallScore());
    }
}
