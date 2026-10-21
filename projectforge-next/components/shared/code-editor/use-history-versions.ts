"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { useFormatContext } from "@/hooks/use-format";
import { historyQueryKey } from "@/hooks/use-history";
import { formatTimestampMinutes } from "@/lib/format";
import { fetchHistory, type HistoryInfo } from "@/lib/rs/history";
import type { CodeVersion, CodeVersionSource } from "./types";

/**
 * The former values of one property of an entity, taken from its change history, for the diff of the
 * editor showing it (CodeDiffDialog): each change of the property yields the value it replaced, shown
 * through [format] (JSON indented, say). The history is the one the history tab reads (same query key),
 * so a save that invalidates it refreshes these as well. Undefined for an entity not loaded yet.
 */
export function useHistoryVersions({
  entity,
  id,
  property,
  format = (value) => value ?? "",
}: {
  entity: string;
  id: number | null | undefined;
  property: string;
  format?: (value: string | null) => string;
}): CodeVersionSource | undefined {
  const t = useTranslations();
  const ctx = useFormatContext();
  const qc = useQueryClient();
  return useMemo(() => {
    if (id == null) return undefined;
    const queryKey = historyQueryKey(entity, id);
    // Fetched with the shared query's own signal: the signal of one caller (a picker unmounted again) would
    // cancel the history for all of them, and the query would only be retried seconds later.
    const history = () =>
      qc.fetchQuery<HistoryInfo>({
        queryKey,
        queryFn: ({ signal }) => fetchHistory(entity, id, signal),
      });
    const changes = async () =>
      (await history()).entries.flatMap((entry) => {
        const attr = entry.attributes.find((a) => a.propertyName === property);
        return attr ? [{ entry, oldValue: attr.oldValue }] : [];
      });
    return {
      queryKey: [...queryKey, "versions", property],
      list: async (): Promise<CodeVersion[]> => {
        const list = await changes();
        return list
          .map(({ entry }) => ({
            key: String(entry.id),
            timestamp: new Date(entry.modifiedAt).getTime() || null,
            label: t("codeEditor.diff.historyVersion", {
              arg0: formatTimestampMinutes(entry.modifiedAt, ctx),
              arg1: entry.modifiedByUser ?? "?",
            }),
          }))
          .sort((a, b) => (b.timestamp ?? 0) - (a.timestamp ?? 0));
      },
      load: async (key) => {
        const change = (await changes()).find(
          ({ entry }) => String(entry.id) === key
        );
        return format(change?.oldValue ?? null);
      },
      emptyText: t("codeEditor.diff.noHistory"),
    };
  }, [entity, id, property, format, qc, t, ctx]);
}
