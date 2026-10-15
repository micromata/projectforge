"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { StatusPill } from "@/components/shared/status-pill";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { CacheEntry } from "@/lib/rs/admin-caches";
import { CACHE_STATE_KEYS, CACHE_STATE_TONES } from "./cache-labels";

/** The state of a cache; since when it refreshes (or is stuck) or why it failed is told by its tooltip. */
export function CacheStatePill({ cache }: { cache: CacheEntry }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const pill = (
    <StatusPill
      tone={CACHE_STATE_TONES[cache.state]}
      label={t(CACHE_STATE_KEYS[cache.state])}
    />
  );
  const since = cache.status.refreshRunningSince;
  const hint =
    cache.state === "FAILED"
      ? cache.status.lastRefreshError
      : (cache.state === "REFRESHING" || cache.state === "STUCK") && since
        ? t("system.caches.runningSince", {
            arg0: formatTimestampMinutes(since, ctx),
          })
        : null;
  if (!hint) return pill;
  return (
    <HintTooltip plain openOnTap text={hint}>
      <span>{pill}</span>
    </HintTooltip>
  );
}
