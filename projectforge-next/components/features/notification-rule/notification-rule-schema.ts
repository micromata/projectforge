import { z } from "zod";
import { NOTIFICATION_RULE_METADATA } from "@/lib/metadata/notification-rule.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { INTEGER, minMarker, REQUIRED } from "@/lib/validation/markers";

/**
 * The fields mirror org.projectforge.rest.notification.NotificationRule, which keeps the JSON columns of
 * NotificationRuleDO (schedule, recipients, delivery) flat. Only the plain columns have metadata; the
 * flat ones are described here, their checks are the backend's (NotificationRuleEntityRest.validate),
 * which answers with these field names. The params of the rule type are `params.<key>`, described and
 * checked by the backend only (see RuleParamsField).
 */
const m = fromMetadata(NOTIFICATION_RULE_METADATA);

const ref = z.object({
  id: z.number(),
  displayName: z.string().optional(),
});

/** A whole, non-negative number which must not be left empty (a Kotlin `Int` of the DTO). */
const count = z
  .number()
  .nullable()
  .refine((v): boolean => v != null, REQUIRED)
  .refine((v) => v == null || Number.isInteger(v), INTEGER)
  .refine((v) => v == null || v >= 0, minMarker(0));

const deliveryStep = z.object({
  channel: z.enum(["IN_APP", "MAIL", "SMS"]),
  delayMinutes: count,
  onlyIfUnacknowledged: z.boolean(),
});

export const notificationRuleSchema = z.object({
  id: z.number().nullable(),
  name: m.requiredString("name"),
  description: m.nullableString("description"),
  active: m.booleanField("active"),
  // One of the types the backend registered (see useRuleTypes), not an enum of the frontend.
  ruleType: z
    .string()
    .nullable()
    .refine((v): boolean => !!v, REQUIRED),
  scheduleMode: z.enum([
    "NONE",
    "WORKING_DAYS_BEFORE_MONTH_END",
    "DAY_OF_MONTH",
    "DAILY",
    "WEEKLY",
  ]),
  scheduleDay: count,
  scheduleDayOfWeek: z
    .enum([
      "MONDAY",
      "TUESDAY",
      "WEDNESDAY",
      "THURSDAY",
      "FRIDAY",
      "SATURDAY",
      "SUNDAY",
    ])
    .nullable(),
  params: z.record(z.string(), z.unknown()),
  recipientGroups: z.array(ref),
  recipientUsers: z.array(ref),
  allEmployees: z.boolean(),
  employeeStatus: z.array(z.string()),
  excludedEmployeeStatus: z.array(z.string()),
  onlyAffected: z.boolean(),
  deliverySteps: z.array(deliveryStep),
  severity: m.enumField("severity"),
  display: m.enumField("display"),
  manualDone: m.booleanField("manualDone"),
  menuBadge: m.nullableString("menuBadge"),
  linkMode: z.enum(["DEFAULT", "NONE", "MENU_ITEM"]),
  linkMenuItem: z.string().nullable(),
  subject: m.requiredString("subject"),
  // The editor writes "" for an empty text, so a required string catches it.
  text: m.requiredString("text"),
  smsText: m.nullableString("smsText"),
  editableByGroups: z.array(ref),
  /** Read-only, sent back untouched (the backend keeps its own value anyway). */
  lastRun: z.string().nullable(),
});

export type NotificationRuleValues = z.infer<typeof notificationRuleSchema>;

/**
 * Field names of the form, so a server validation error naming another one becomes a toast; `params.*`
 * for the params of the rule type (`params.<key>`).
 */
export const NOTIFICATION_RULE_FIELDS: readonly string[] = [
  ...Object.keys(notificationRuleSchema.shape),
  "params.*",
];

export const NOTIFICATION_RULE_ARRAY_FIELDS = [
  "recipientGroups",
  "recipientUsers",
  "employeeStatus",
  "excludedEmployeeStatus",
  "deliverySteps",
  "editableByGroups",
] as const;
