"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { useHistory } from "@/hooks/use-history";
import { HistoryTimeline } from "./history-timeline";

export interface HistorySectionProps {
  /** Entity category as the backend maps it, e.g. "book" — the url is /rs/{entity}/history/{id}. */
  entity: string;
  entityId: number;
}

/**
 * The change history of one entity, ready to drop onto a page.
 *
 * Fetches on mount, so render it only where the history is actually shown (its own tab/route):
 * building the history of a long-lived entity is expensive on the server.
 */
export function HistorySection({ entity, entityId }: HistorySectionProps) {
  const t = useTranslations();
  const { data, isLoading, isError } = useHistory(entity, entityId);

  return (
    <SectionCard>
      <SectionHeader title={t("label.historyOfChanges")} />
      {isLoading ? (
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      ) : isError || !data ? (
        <p className="text-sm text-muted-foreground">
          {t("validation.error.generic")}
        </p>
      ) : (
        <HistoryTimeline
          entries={data.entries}
          supportsUserComments={data.supportsUserComments}
          entity={entity}
          entityId={entityId}
        />
      )}
    </SectionCard>
  );
}
