/**
 * The notification system: the user's own notifications shown in the app
 * (`org.projectforge.rest.notification.NotificationRest`) and the services of the rule editor besides the
 * standard entity endpoints (`NotificationRuleEntityRest`, admins and finance only).
 */

import { request } from "./client";

export type NotificationSeverity = "INFO" | "IMPORTANT" | "URGENT";

export type NotificationDisplay = "TOAST" | "TOAST_CONFIRM" | "BANNER";

export type NotificationStatus =
  | "OPEN"
  | "ACKNOWLEDGED"
  | "DONE"
  | "RESOLVED"
  | "EXPIRED";

export type NotificationChannel = "IN_APP" | "MAIL" | "SMS";

export type NotificationRuleType =
  | "TIMESHEETS_MISSING"
  | "VACATION_LEFT"
  | "PASSWORD_NOT_CHANGED"
  | "TIMESHEETS_WITHOUT_KOST2"
  | "TIMESHEETS_TO_ACCEPT"
  | "CREDITOR_INVOICES_DUE"
  | "MANUAL";

/** `NotificationDao.Summary`, sent with the user status (see useAuth). */
export interface NotificationSummary {
  openCount: number;
  maxSeverity?: NotificationSeverity | null;
  latestId?: number | null;
}

/** `NotificationRest.Notification`: one visible in the app. */
export interface AppNotification {
  id: number;
  severity: NotificationSeverity;
  display: NotificationDisplay;
  status: NotificationStatus;
  /** May the recipient mark it as done (a banner is otherwise finished by its rule only)? */
  manualDone: boolean;
  title?: string | null;
  /** Sanitized HTML. */
  body?: string | null;
  /** A path of the app, e.g. `next/monthlyEmployeeReport`. */
  link?: string | null;
  created?: string | null;
  acknowledgedAt?: string | null;
  doneAt?: string | null;
  resolvedAt?: string | null;
}

/** The notifications of the logged-in user visible in the app, the newest first. */
export function fetchMyNotifications(
  signal?: AbortSignal
): Promise<AppNotification[]> {
  return request<AppNotification[]>(
    "/rs/notification/my",
    { method: "GET" },
    signal
  );
}

/**
 * The notifications of the logged-in user no longer visible in the app (confirmed, done, resolved or
 * expired) of the last days, the newest first.
 */
export function fetchMyArchivedNotifications(
  signal?: AbortSignal
): Promise<AppNotification[]> {
  return request<AppNotification[]>(
    "/rs/notification/my/archived",
    { method: "GET" },
    signal
  );
}

/** Confirms the notification: no further escalation; a banner stays until done. */
export function acknowledgeNotification(id: number): Promise<AppNotification> {
  return request<AppNotification>(`/rs/notification/${id}/acknowledge`, {
    method: "POST",
  });
}

export function markNotificationDone(id: number): Promise<AppNotification> {
  return request<AppNotification>(`/rs/notification/${id}/done`, {
    method: "POST",
  });
}

// --- Rule editor ---

/** A variable `{{key}}` of the texts of a rule type, with its translated label. */
export interface NotificationVariable {
  key: string;
  label: string;
  /** Available for every rule type (the names of the recipient), otherwise only for the given one. */
  common?: boolean;
  /** How a value looks (e.g. a list of invoices), from synthetic data. */
  example?: string | null;
}

export interface MenuBadgeOption {
  id: string;
  label: string;
}

export interface NotificationPreview {
  recipientCount: number;
  /** The first recipients' names. */
  recipients: string[];
  alreadyNotified: number;
  periodKey?: string | null;
  /** Rendered with the values of the logged-in user. */
  subject: string;
  /** Sanitized HTML, rendered with the values of the logged-in user. */
  text: string;
  /** Rendered with example values (the own ones may be empty, e.g. no unbooked days). */
  exampleSubject: string;
  /** Sanitized HTML, rendered with example values. */
  exampleText: string;
  /** The SMS as sent (own and example values), absent if the cascade has no SMS step. */
  sms?: string | null;
  exampleSms?: string | null;
}

/** Is SMS configured (otherwise SMS steps are skipped), and the max length of an SMS. */
export interface NotificationSmsInfo {
  configured: boolean;
  maxLength: number;
}

export interface NotificationTestResult {
  delivered: NotificationChannel[];
  failed: Partial<Record<NotificationChannel, string>>;
}

export interface NotificationTriggerResult {
  recipients: number;
  created: number;
  errors: number;
  lastError?: string | null;
}

export interface NotificationLogEntry {
  id: number;
  /** Null for an ad hoc notification or a deleted rule. */
  ruleName?: string | null;
  recipient?: string | null;
  status: NotificationStatus;
  periodKey?: string | null;
  title?: string | null;
  created?: string | null;
  inAppSince?: string | null;
  acknowledgedAt?: string | null;
  acknowledgedBy?: string | null;
  doneAt?: string | null;
  resolvedAt?: string | null;
  deliverySteps: {
    channel: NotificationChannel;
    dueAt?: string | null;
    sentAt?: string | null;
    skipped: boolean;
    failed: boolean;
    attempts: number;
    error?: string | null;
  }[];
  /** May the logged-in user end it (pending and editor of its rule)? */
  finishable?: boolean;
}

export function fetchNotificationVariables(
  ruleType: NotificationRuleType | null,
  signal?: AbortSignal
): Promise<NotificationVariable[]> {
  const query = ruleType ? `?ruleType=${encodeURIComponent(ruleType)}` : "";
  return request<NotificationVariable[]>(
    `/rs/notificationRule/variables${query}`,
    { method: "GET" },
    signal
  );
}

export function fetchMenuBadgeOptions(
  signal?: AbortSignal
): Promise<MenuBadgeOption[]> {
  return request<MenuBadgeOption[]>(
    "/rs/notificationRule/menuBadges",
    { method: "GET" },
    signal
  );
}

export function fetchNotificationSmsInfo(
  signal?: AbortSignal
): Promise<NotificationSmsInfo> {
  return request<NotificationSmsInfo>(
    "/rs/notificationRule/smsInfo",
    { method: "GET" },
    signal
  );
}

/** The recipients and texts of the given, maybe unsaved rule (the values of the form). */
export function previewNotificationRule(
  rule: object
): Promise<NotificationPreview> {
  return request<NotificationPreview>("/rs/notificationRule/preview", {
    method: "POST",
    body: JSON.stringify(rule),
  });
}

/** Sends the given, maybe unsaved rule to the logged-in user only, through all channels at once. */
export function testNotificationRuleToMe(
  rule: object
): Promise<NotificationTestResult> {
  return request<NotificationTestResult>("/rs/notificationRule/testToMe", {
    method: "POST",
    body: JSON.stringify(rule),
  });
}

/** Runs the saved rule now, regardless of its schedule. */
export function triggerNotificationRule(
  id: number
): Promise<NotificationTriggerResult> {
  return request<NotificationTriggerResult>(
    `/rs/notificationRule/${id}/trigger`,
    { method: "POST" }
  );
}

export function fetchNotificationLog(
  ruleId: number,
  signal?: AbortSignal
): Promise<NotificationLogEntry[]> {
  return request<NotificationLogEntry[]>(
    `/rs/notificationRule/${ruleId}/notifications`,
    { method: "GET" },
    signal
  );
}

/** Ends the pending notification (expired), for editors of its rule. */
export function finishNotification(id: number): Promise<NotificationLogEntry> {
  return request<NotificationLogEntry>(
    `/rs/notificationRule/notifications/${id}/finish`,
    { method: "POST" }
  );
}

/** Ends all pending notifications of the rule (expired), for its editors. */
export function finishPendingNotifications(
  ruleId: number
): Promise<{ finished: number }> {
  return request<{ finished: number }>(
    `/rs/notificationRule/${ruleId}/finishPending`,
    { method: "POST" }
  );
}

export interface NotificationLogFilter {
  recipientId?: number | null;
  status?: NotificationStatus | null;
}

/** The notifications of all rules, the newest first (at most the last 500), for admins and finance. */
export function fetchAllNotificationLog(
  filter: NotificationLogFilter,
  signal?: AbortSignal
): Promise<NotificationLogEntry[]> {
  const params = new URLSearchParams();
  if (filter.recipientId) params.set("recipientId", String(filter.recipientId));
  if (filter.status) params.set("status", filter.status);
  const query = params.size > 0 ? `?${params}` : "";
  return request<NotificationLogEntry[]>(
    `/rs/notificationRule/log${query}`,
    { method: "GET" },
    signal
  );
}
