import type { StatusTone } from "@/components/shared/status-pill";
import type {
  LogAudience,
  LogCategory,
  LogGroupEntry,
  LogGroupScope,
  LogGroupStatus,
  LogGroupStatusFilter,
  LogNotify,
} from "@/lib/rs/admin-errors";

// The keys are spelled out: the i18n scan of `gen` finds literal keys only.

export const CATEGORY_KEYS: Record<LogCategory, string> = {
  EXTERNAL: "system.admin.adminErrors.category.external",
  SECURITY: "system.admin.adminErrors.category.security",
  DATA: "system.admin.adminErrors.category.data",
  BUG: "system.admin.adminErrors.category.bug",
  CONFIG: "system.admin.adminErrors.category.config",
  CLIENT: "system.admin.adminErrors.category.client",
  UNCLASSIFIED: "system.admin.adminErrors.category.unclassified",
};

/** In the order of `LogCategory`: the most urgent first. */
export const CATEGORIES = Object.keys(CATEGORY_KEYS) as LogCategory[];

export const STATUS_KEYS: Record<LogGroupStatus, string> = {
  NEW: "system.admin.adminErrors.status.new",
  ACKNOWLEDGED: "system.admin.adminErrors.status.acknowledged",
  IGNORED: "system.admin.adminErrors.status.ignored",
  RESOLVED: "system.admin.adminErrors.status.resolved",
};

export const STATUS_FILTER_KEYS: Record<LogGroupStatusFilter, string> = {
  OPEN: "system.admin.adminErrors.status.open",
  NEW: "system.admin.adminErrors.status.new",
  ACKNOWLEDGED: "system.admin.adminErrors.status.acknowledged",
  IGNORED: "system.admin.adminErrors.status.ignored",
  RESOLVED: "system.admin.adminErrors.status.resolved",
  ALL: "system.admin.adminErrors.status.all",
};

/** The scope's chip shows the title of its key figure. */
export const SCOPE_KEYS: Record<LogGroupScope, string> = {
  NEW_24H: "system.admin.adminErrors.kpi.newProblems",
  REGRESSION: "system.admin.adminErrors.kpi.regressions",
};

export const NOTIFY_KEYS: Record<LogNotify, string> = {
  NONE: "system.admin.adminErrors.notify.none",
  DIGEST: "system.admin.adminErrors.notify.digest",
  DIGEST_IF_NEW: "system.admin.adminErrors.notify.digestIfNew",
  IMMEDIATE: "system.admin.adminErrors.notify.immediate",
};

export const AUDIENCE_KEYS: Record<LogAudience, string> = {
  DEVELOPER: "system.admin.adminErrors.audience.developer",
  ADMIN: "system.admin.adminErrors.audience.admin",
  SECURITY: "system.admin.adminErrors.audience.security",
};

/** New problems stand out, regressions most; looked at ones stay quiet. */
export function statusTone(entry: LogGroupEntry): StatusTone {
  if (entry.regression) return "danger";
  switch (entry.status) {
    case "NEW":
      return "info";
    case "RESOLVED":
      return "success";
    default:
      return "neutral";
  }
}
