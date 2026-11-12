"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete02Icon,
  PlayIcon,
  StopIcon,
  Timer02Icon,
} from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useFormatContext } from "@/hooks/use-format";
import { formatTime } from "@/lib/format";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import { useTick, useTimesheetTimer } from "@/hooks/use-timesheet-timer";
import { ceilToGrid, floorToGrid, timeGridOf } from "@/lib/timesheet-time-grid";

/** `1:05:09` for one hour, five minutes and nine seconds. */
function elapsedText(ms: number): string {
  const seconds = Math.max(0, Math.floor(ms / 1000));
  const two = (n: number) => String(n).padStart(2, "0");
  return `${Math.floor(seconds / 3600)}:${two(Math.floor(seconds / 60) % 60)}:${two(seconds % 60)}`;
}

/**
 * The timer of the time sheets, shared with the app: start it here or there, stop it on either. Stopping opens a new
 * time sheet with the start rounded down and the end rounded up to the user's time grid; the timer is discarded once
 * that sheet is saved (see TimesheetEditPageClient), so cancelling the form keeps it running.
 */
export function TimesheetTimerButton() {
  const t = useTranslations();
  const router = useRouter();
  const { user } = useAuth();
  const ctx = useFormatContext();
  const enabled = useFeatureEnabled("TIMESHEET_LIST");
  const { timer, start, discard, isBusy, serverNow } =
    useTimesheetTimer(enabled);
  useTick(timer != null, 1_000);
  if (!enabled) return null;

  if (!timer) {
    return (
      <HintTooltip text={t("timesheet.timer.start.tooltip")}>
        <Button
          variant="ghost"
          size="sm"
          className="shrink-0"
          aria-label={t("timesheet.timer.start")}
          data-testid="timesheet-timer-start"
          disabled={isBusy}
          onClick={start}
        >
          <HugeiconsIcon icon={PlayIcon} size={18} />
        </Button>
      </HintTooltip>
    );
  }

  const startedAt = timer.startedAt ?? serverNow();
  function stop() {
    const step = timeGridOf(user?.timesheetTimeGridMinutes);
    const from = floorToGrid(startedAt, step);
    let to = ceilToGrid(serverNow(), step);
    if (to <= from) to = from + step * 60_000;
    const params = new URLSearchParams({
      startDate: String(Math.floor(from / 1000)),
      endDate: String(Math.floor(to / 1000)),
      fromTimer: "1",
    });
    (
      [
        ["taskId", timer?.taskId],
        ["kost2Id", timer?.kost2Id],
        ["description", timer?.description],
        ["location", timer?.location],
        ["reference", timer?.reference],
        ["tag", timer?.tag],
      ] as const
    ).forEach(([name, value]) => {
      if (value != null && value !== "") params.set(name, String(value));
    });
    router.push(`/timesheet/new?${params.toString()}`);
  }

  return (
    <div
      className="mr-2 flex shrink-0 items-center gap-2 rounded-md border-l-4 border-warning bg-muted py-0.5 pr-1 pl-2"
      data-testid="timesheet-timer"
    >
      <HugeiconsIcon icon={Timer02Icon} size={18} className="text-warning" />
      <span
        className="text-sm font-semibold tabular-nums"
        data-testid="timesheet-timer-elapsed"
      >
        {elapsedText(serverNow() - startedAt)}
      </span>
      <span className="hidden text-xs text-muted-foreground md:inline">
        {t("timesheet.timer.since", { arg0: formatTime(startedAt, ctx) })}
      </span>
      <HintTooltip text={t("timesheet.timer.discard.tooltip")}>
        <Button
          variant="ghost"
          size="icon"
          className="size-7 text-muted-foreground"
          aria-label={t("timesheet.timer.discard")}
          disabled={isBusy}
          onClick={discard}
        >
          <HugeiconsIcon icon={Delete02Icon} size={16} />
        </Button>
      </HintTooltip>
      <HintTooltip text={t("timesheet.timer.stop.tooltip")}>
        <Button
          size="sm"
          className="gap-1 bg-warning text-black hover:bg-warning/90"
          aria-label={t("timesheet.timer.stop")}
          data-testid="timesheet-timer-stop"
          onClick={stop}
        >
          <HugeiconsIcon icon={StopIcon} size={14} />
          {t("timesheet.timer.stop.button")}
        </Button>
      </HintTooltip>
    </div>
  );
}
