"use client";

import type { CSSProperties } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { RefreshIcon } from "@hugeicons/core-free-icons";
import { PULL_THRESHOLD, usePullToRefresh } from "@/hooks/use-pull-to-refresh";
import { cn } from "@/lib/utils";

/**
 * The badge of the pull to refresh in the app (see {@link usePullToRefresh}): comes down from the top with the
 * finger, turns with the pull, and spins while the page reloads. Nothing outside the app or without a pull.
 */
export function PullToRefreshIndicator() {
  const { distance, reloading } = usePullToRefresh();
  if (distance === null) return null;
  const ready = distance >= PULL_THRESHOLD;
  // Driven by CSS variables: the position and the turn follow the finger on every move.
  const style = {
    "--pull-offset": `${Math.min(distance, PULL_THRESHOLD * 1.5) - 40}px`,
    "--pull-turn": `${(distance / PULL_THRESHOLD) * 270}deg`,
  } as CSSProperties;
  return (
    <div
      aria-hidden
      style={style}
      className="pointer-events-none fixed top-0 left-1/2 z-50 -translate-x-1/2 translate-y-(--pull-offset) print:hidden"
    >
      <div
        className={cn(
          "flex size-10 items-center justify-center rounded-full border bg-background shadow-md",
          ready ? "text-primary" : "text-muted-foreground"
        )}
      >
        <HugeiconsIcon
          icon={RefreshIcon}
          strokeWidth={2}
          className={cn(
            "size-5",
            reloading ? "animate-spin" : "rotate-(--pull-turn)"
          )}
        />
      </div>
    </div>
  );
}
