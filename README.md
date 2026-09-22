cornalix-ms-scoring — service de calcul du score de maturité

Rôle dans l'architecture
Deuxième microservice **métier** de Cornalix (voir la proposition d'architecture, §5 : cycle de valeur). Il calcule le score de maturité d'une organisation à partir de ses réponses au questionnaire de diagnostic, agrégé par fonction NIST CSF 2.0 (Govern, Identify, Protect, Detect, Respond, Recover). Il ne possède aucune donnée lui-même : il consomme `cornalix-ms-diagnostic` en HTTP à chaque appel.

Ce qu'il fait (dans son périmètre, MVP)

Exposer le score courant d'une organisation, calculé à la volée (rien n'est mis en cache ni stocké).
Score par fonction NIST CSF 2.0 = moyenne des réponses répondues dans cette fonction (`YES`=1, `PARTIAL`=0.5, `NO`=0, `NOT_APPLICABLE` exclu du calcul). Les questions non répondues sont exclues, pas comptées comme 0.

Ce qu'il ne fait PAS (hors périmètre, volontairement)

Recommandations générées à partir du score — service séparé « Recommandation IA » / Cornalix Guard, Phase 2.
Historique ou tendance du score dans le temps.
Score par contrôle CIS (seulement par fonction NIST pour ce premier récit).
Gérer l'authentification, les organisations, le catalogue de questions ou les réponses — `cornalix-ms-identity` et `cornalix-ms-diagnostic`.

Sécurité

Comme tous les microservices Cornalix, ce service ne gère aucun mot de passe : il valide les jetons JWT émis par le même User Pool Amazon Cognito que `cornalix-ms-identity` (`SecurityConfig`, JWKS). Toute route (hors `/actuator/health` et `/actuator/info`) exige un jeton valide, et l'accès au score d'une organisation est vérifié contre le `tenant_id`/`tenant_scope` du jeton — même garde-fou que `/api/v1/organizations/{id}` sur `cornalix-ms-identity` (SCRUM-6). Le jeton de l'appelant est transmis tel quel à `cornalix-ms-diagnostic` lors des appels HTTP sous-jacents.

Suivi Jira

Epic : [SCRUM-8 — Cornalix — Scoring (MVP)](https://jservicesconseils.atlassian.net/browse/SCRUM-8), projet SCRUM.

Développer en local

```
mvn spring-boot:run
```

Démarre sur `http://localhost:8083` (8080 = core-api, 8081 = `cornalix-ms-identity`, 8082 = `cornalix-ms-diagnostic`, qui doit tourner en parallèle pour que ce service puisse calculer un score). Nécessite un jeton Cognito valide (même pool que les deux autres services) pour toute route protégée.
