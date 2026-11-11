# Notification rules: adding a rule type

A rule type is one Spring bean implementing `NotificationRuleHandler` (`projectforge-business`,
`org.projectforge.business.notification`), in the core or in a plugin. Being a bean registers it
(`NotificationRuleRegistry`); the service, the REST endpoint (`NotificationRuleEntityRest`) and the editor of next
(`components/features/notification-rule`) know no single type. Examples: `PasswordNotChangedRuleHandler` (one number),
`CvrTeamCheckUnansweredRuleHandler` (CVE-Clinic plugin, in its backend repository: a case to pick, only those concerned are notified).

## What the handler declares
- `ruleType`: the key stored in `t_notification_rule.rule_type` (at most 50 characters, unique, never renamed: stored
  rules refer to it). A `const val TYPE` in the companion, for code referring to the type.
- `i18nKey` (default `notification.ruleTypes.<key>`) and `order` (position in the select of the editor).
- `params`: the settings, `NotificationRuleParam` (`IntParam` with range, `BooleanParam`, `SelectParam` (`ofEnum` for an
  enum), `EntityParam` with the rest category of its autocompletion). Declared as `val` in the companion, read by the
  handler with `PARAM.of(rule)`. Labels `notification.params.<key>`, options `notification.params.<key>.<VALUE>`. The
  values are stored flat in the JSON of `params` (`NotificationParams.values`), so a new param needs no migration. The
  service checks them (required, range, option); `paramErrors` adds the type's own checks (e.g. the case is open),
  `paramDisplayNames` the names of the entities.
- Recipients: `recipientMode` (`RECIPIENTS`, `AFFECTED_AND_COPY`, `AFFECTED_ONLY`) with `recipientsHintKey` for the
  editor, `requiresRecipients`; the run decides by `Evaluation.affectedAreRecipients`.
- `templateSubjectKey`/`templateTextKey`: the texts of a new rule (variables as `[key]` in the bundle).
- `isManualDoneForced(display)`, `warnings(rule)` (after saving), `isAvailable` (a plugin type: only while its plugin
  is active; otherwise its rules wait and the editor doesn't offer it).
- And the evaluation: `variables`, `evaluate`, `variables(rule, today, user)`, `exampleVariables`, `isRelevant`,
  `resolve`.

## Texts
A plugin keeps the texts of its types in its own bundle (e.g. `CveRadarI18nResources(_de).properties`), with the keys
above (prefix `notification.`, exported to next by `GenerateNextI18nMessagesMain`). Then `bin/pfDev.sh gen`.

## Tests
A handler test like `CvrTeamCheckUnansweredRuleHandlerTest` (evaluate, resolve, params via the REST endpoint);
`NotificationRuleRegistryTest` covers reading the stored params.
