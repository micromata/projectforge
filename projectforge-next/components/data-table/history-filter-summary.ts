import { type FormatContext, formatTimestampMinutes } from "@/lib/format";
import { fromLikeTerm, type FilterValues } from "./filter-value";
import { HISTORY_FILTER_FIELDS } from "./history-filter";

/**
 * What the combined history pill reads: "Kai Reinhard, 15.07.2026, 10:30 – 16.07.2026, 23:59, Titel".
 *
 * The per-field [describeFilterValue] cannot do this — the pill spans three values — and the dates
 * have to go through lib/format.ts, so the pill shows the same layout and time zone as the table
 * beside it.
 */
export function describeHistoryFilter(
  values: FilterValues,
  ctx: FormatContext
): string {
  const parts: string[] = [];

  const user = values[HISTORY_FILTER_FIELDS.user];
  // The name the autocomplete stored; a filter restored from a favorite always carries it, because
  // MagicFilter.init resolves it server-side.
  if (user?.displayName) parts.push(user.displayName);
  else if (user?.id != null) parts.push(`#${user.id}`);

  const interval = values[HISTORY_FILTER_FIELDS.interval];
  if (interval?.from || interval?.to) {
    const from = formatTimestampMinutes(interval.from, ctx);
    const to = formatTimestampMinutes(interval.to, ctx);
    // A half-open interval reads as "from …" / "… to", the ellipsis standing for the open end.
    parts.push(
      from && to ? `${from} – ${to}` : from ? `${from} – …` : `… – ${to}`
    );
  }

  const value = values[HISTORY_FILTER_FIELDS.value];
  if (value?.value) parts.push(fromLikeTerm(value.value));

  return parts.join(", ");
}
