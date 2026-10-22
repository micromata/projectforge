"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import type { PollListRow } from "./types";

/** The title of a poll in the list, a template marked as such: it is listed among the polls nobody answers. */
export function PollTitleCell({ row }: { row: PollListRow }) {
  const t = useTranslations();
  return (
    <span className="flex min-w-0 items-center gap-1.5">
      <span className="truncate">{row.title}</span>
      {row.template && (
        <Badge variant="secondary" className="shrink-0 font-normal">
          {t("poll.state.template")}
        </Badge>
      )}
    </span>
  );
}
