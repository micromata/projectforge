"use client";

import { useTranslations } from "next-intl";
import { Sparkline } from "@/components/shared/chart/sparkline";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { StatusPill, type StatusTone } from "@/components/shared/status-pill";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber, formatTimestampMinutes } from "@/lib/format";
import type {
  LogGroupCounts,
  SubsystemEntry,
  SubsystemState,
} from "@/lib/rs/admin-errors";
import { cn } from "@/lib/utils";
import { AdminHandledCounts } from "./admin-handled-counts";

const STATE_TONES: Record<SubsystemState, StatusTone> = {
  OK: "success",
  UNKNOWN: "neutral",
  DEGRADED: "danger",
  DOWN: "danger",
};

const STATE_KEYS: Record<SubsystemState, string> = {
  OK: "system.admin.adminErrors.subsystem.state.ok",
  UNKNOWN: "system.admin.adminErrors.subsystem.state.unknown",
  DEGRADED: "system.admin.adminErrors.subsystem.state.degraded",
  DOWN: "system.admin.adminErrors.subsystem.state.down",
};

/**
 * One tile per active subsystem or interface (LDAP, IdP, gateway, Sipgate, ...): its state, the statistics of its
 * active problems (the resolved, ignored and muted ones listed below) and their trend. A click shows its problems
 * ([onOpen]).
 */
export function AdminSubsystemTiles({
  subsystems,
  onOpen,
}: {
  subsystems: SubsystemEntry[];
  onOpen: (subsystem: SubsystemEntry) => void;
}) {
  const t = useTranslations();
  if (subsystems.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("system.admin.adminErrors.subsystems.none")}
      </p>
    );
  }
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {subsystems.map((subsystem) => (
        <SubsystemTile
          key={subsystem.id}
          subsystem={subsystem}
          onOpen={() => onOpen(subsystem)}
        />
      ))}
    </div>
  );
}

function SubsystemTile({
  subsystem,
  onOpen,
}: {
  subsystem: SubsystemEntry;
  onOpen: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const lastRun = Math.max(0, ...subsystem.syncs.map((s) => s.lastRun ?? 0));
  const figure = (label: string, counts: LogGroupCounts, alert: boolean) => (
    <div>
      <div className="text-xs text-muted-foreground">{label}</div>
      <div
        className={cn(
          "text-xl font-semibold tabular-nums",
          alert && counts.active > 0 && "text-destructive"
        )}
      >
        {formatNumber(counts.active, ctx, 0)}
      </div>
      <AdminHandledCounts counts={counts} />
    </div>
  );
  return (
    <HintTooltip content={<SyncsHint subsystem={subsystem} />}>
      <button
        type="button"
        onClick={onOpen}
        className={cn(
          "flex flex-col gap-2 rounded-md border px-3 py-2 text-left transition-colors hover:bg-muted/50",
          subsystem.state === "DOWN" && "border-destructive"
        )}
      >
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <div className="truncate font-medium">{subsystem.title}</div>
            <div className="truncate text-xs text-muted-foreground">
              {subsystem.detail || " "}
            </div>
          </div>
          <StatusPill
            tone={STATE_TONES[subsystem.state]}
            label={t(STATE_KEYS[subsystem.state])}
          />
        </div>
        <div className="flex items-end justify-between gap-3">
          <div className="flex gap-4">
            {figure(
              t("system.admin.adminErrors.kpi.occurrences"),
              subsystem.occurrences24h,
              true
            )}
            {figure(
              t("system.admin.adminErrors.status.open"),
              subsystem.open,
              false
            )}
          </div>
          <Sparkline
            values={subsystem.trend}
            ariaLabel={t("system.admin.adminErrors.trend")}
          />
        </div>
        {lastRun > 0 && (
          <div className="text-xs text-muted-foreground">
            {t("system.admin.adminErrors.subsystems.lastRun")}:{" "}
            {formatTimestampMinutes(lastRun, ctx)}
          </div>
        )}
      </button>
    </HintTooltip>
  );
}

/** The last run of each sync and its last error, and what a click does. */
function SyncsHint({ subsystem }: { subsystem: SubsystemEntry }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <div className="space-y-1">
      {subsystem.syncs.map((sync) => (
        <div key={sync.type}>
          <div className="font-medium">
            {sync.type}: {sync.lastStatus?.toLowerCase()}
            {sync.lastRun
              ? `, ${formatTimestampMinutes(sync.lastRun, ctx)}`
              : ""}
          </div>
          {sync.lastError && (
            <div className="break-words">
              {t("system.admin.adminErrors.subsystems.lastError")}
              {sync.lastErrorDate
                ? ` (${formatTimestampMinutes(sync.lastErrorDate, ctx)})`
                : ""}
              : {sync.lastError}
            </div>
          )}
        </div>
      ))}
      <div>{t("system.admin.adminErrors.subsystems.tooltip")}</div>
    </div>
  );
}
