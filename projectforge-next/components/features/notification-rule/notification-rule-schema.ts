import { z } from "zod";
import { NOTIFICATION_RULE_METADATA } from "@/lib/metadata/notification-rule.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { INTEGER, minMarker, REQUIRED } from "@/lib/validation/markers";

/**
 * The fields mirror org.projectforge.rest.notification.NotificationRule, which keeps the JSON columns of
 * NotificationRuleDO (schedule, params, recipients, delivery) flat. Only the plain columns have
 * metadata; the flat ones are described here, their checks are the backend's
 * (NotificationRuleEntityRest.validate), which answers with these field names.
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
  ruleType: m.enumField("ruleType"),
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
  referredMonth: z.enum(["CURRENT", "PREVIOUS"]),
  vacationExpiry: z.enum(["CARRY_OVER", "YEAR_END"]),
  daysBeforeExpiry: count,
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
  subject: m.requiredString("subject"),
  // The editor writes "" for an empty text, so a required string catches it.
  text: m.requiredString("text"),
  editableByGroups: z.array(ref),
  /** Read-only, sent back untouched (the backend keeps its own value anyway). */
  lastRun: z.string().nullable(),
});

export type NotificationRuleValues = z.infer<typeof notificationRuleSchema>;

/** Field names of the form, so a server validation error naming another one becomes a toast. */
export const NOTIFICATION_RULE_FIELDS = Object.keys(
  notificationRuleSchema.shape
) as readonly (keyof NotificationRuleValues)[];

export const NOTIFICATION_RULE_ARRAY_FIELDS = [
  "recipientGroups",
  "recipientUsers",
  "employeeStatus",
  "excludedEmployeeStatus",
  "deliverySteps",
  "editableByGroups",
] as const;
