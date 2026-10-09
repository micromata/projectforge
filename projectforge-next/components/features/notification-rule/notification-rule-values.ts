import type { NotificationRuleValues } from "./notification-rule-schema";
import type { NotificationRuleDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here. The defaults are the DTO's (NotificationRule).
 */
export function toFormValues(
  rule: NotificationRuleDetail
): NotificationRuleValues {
  return {
    id: rule.id ?? null,
    name: rule.name ?? "",
    description: rule.description ?? null,
    active: rule.active ?? false,
    ruleType: rule.ruleType ?? null,
    scheduleMode: rule.scheduleMode ?? "NONE",
    scheduleDay: rule.scheduleDay ?? 0,
    scheduleDayOfWeek: rule.scheduleDayOfWeek ?? null,
    referredMonth: rule.referredMonth ?? "CURRENT",
    vacationExpiry: rule.vacationExpiry ?? "CARRY_OVER",
    daysBeforeExpiry: rule.daysBeforeExpiry ?? 30,
    recipientGroups: rule.recipientGroups ?? [],
    recipientUsers: rule.recipientUsers ?? [],
    allEmployees: rule.allEmployees ?? false,
    employeeStatus: rule.employeeStatus ?? [],
    excludedEmployeeStatus: rule.excludedEmployeeStatus ?? [],
    onlyAffected: rule.onlyAffected ?? true,
    deliverySteps: (rule.deliverySteps ?? []).map((step) => ({
      channel: step.channel ?? "IN_APP",
      delayMinutes: step.delayMinutes ?? 0,
      onlyIfUnacknowledged: step.onlyIfUnacknowledged ?? false,
    })),
    severity: rule.severity ?? "INFO",
    display: rule.display ?? "BANNER",
    manualDone: rule.manualDone ?? false,
    menuBadge: rule.menuBadge ?? null,
    subject: rule.subject ?? "",
    text: rule.text ?? "",
    editableByGroups: rule.editableByGroups ?? [],
    lastRun: rule.lastRun ?? null,
  };
}

/** A new rule: shown in the app at once, as a banner until done. */
export function emptyNotificationRuleValues(): NotificationRuleValues {
  return toFormValues({
    id: null,
    active: true,
    deliverySteps: [
      { channel: "IN_APP", delayMinutes: 0, onlyIfUnacknowledged: false },
    ],
  });
}
