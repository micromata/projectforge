import type { NotificationSeverity } from "@/lib/rs/notification";

/** The colours of a banner (and of the bell's entries) by severity. */
export const SEVERITY_CLASSES: Record<NotificationSeverity, string> = {
  INFO: "border-status-info-border bg-status-info-bg text-foreground",
  IMPORTANT: "border-warning bg-warning/15 text-foreground",
  URGENT: "border-destructive bg-destructive text-white",
};

const RANK: Record<NotificationSeverity, number> = {
  INFO: 0,
  IMPORTANT: 1,
  URGENT: 2,
};

/** The most severe first, the newest first within a severity (the order the backend sends). */
export function bySeverity<T extends { severity: NotificationSeverity }>(
  notifications: T[]
): T[] {
  return [...notifications].sort((a, b) => RANK[b.severity] - RANK[a.severity]);
}
