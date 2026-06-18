"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { HistoryTimeline } from "../history-timeline";
import { useBookHistory } from "../use-book-history";

interface Props {
  bookId: number;
}

export function VerlaufSection({ bookId }: Props) {
  const t = useTranslations("books.edit");
  const { data, isLoading } = useBookHistory(bookId);

  return (
    <SectionCard>
      <SectionHeader title={t("sections.history")} />
      {isLoading ? (
        <p className="text-sm text-muted-foreground">{t("history.loading")}</p>
      ) : data && data.length > 0 ? (
        <HistoryTimeline entries={data} />
      ) : (
        <p className="text-sm text-muted-foreground">{t("history.empty")}</p>
      )}
    </SectionCard>
  );
}
