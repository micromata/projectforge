// Mirrors org.projectforge.rest.notification.NotificationRule (projectforge-rest). Keep field names in
// sync with the Spring DTO.

import type {
  NotificationChannel,
  NotificationDisplay,
  NotificationRuleType,
  NotificationSeverity,
} from "@/lib/rs/notification";

export type ScheduleMode =
  | "NONE"
  | "WORKING_DAYS_BEFORE_MONTH_END"
  | "DAY_OF_MONTH"
  | "DAILY"
  | "WEEKLY";

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

/** A group or user as the DTO references it (`Group` / `User` with the id and display name). */
export interface Ref {
  id: number;
  displayName?: string;
}

/** `NotificationDeliveryStep`: one step of the escalation cascade. */
export interface DeliveryStep {
  channel: NotificationChannel;
  delayMinutes: number;
  onlyIfUnacknowledged: boolean;
}

/**
 * Optional properties are `?`: Spring's mapper leaves out null values (`JsonInclude.Include.NON_NULL`).
 * `toFormValues` normalises that away.
 */
export interface NotificationRuleDetail {
  id: number | null;
  name?: string | null;
  description?: string | null;
  active?: boolean;
  ruleType?: NotificationRuleType | null;
  scheduleMode?: ScheduleMode;
  scheduleDay?: number;
  scheduleDayOfWeek?: DayOfWeek | null;
  referredMonth?: "CURRENT" | "PREVIOUS";
  vacationExpiry?: "CARRY_OVER" | "YEAR_END";
  daysBeforeExpiry?: number;
  recipientGroups?: Ref[];
  recipientUsers?: Ref[];
  allEmployees?: boolean;
  employeeStatus?: string[];
  excludedEmployeeStatus?: string[];
  onlyAffected?: boolean;
  deliverySteps?: DeliveryStep[];
  severity?: NotificationSeverity;
  display?: NotificationDisplay;
  manualDone?: boolean;
  menuBadge?: string | null;
  subject?: string | null;
  text?: string | null;
  editableByGroups?: Ref[];
  /** Read-only: the last run by schedule. */
  lastRun?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

export interface NotificationRuleListRow extends NotificationRuleDetail {
  id: number;
}
