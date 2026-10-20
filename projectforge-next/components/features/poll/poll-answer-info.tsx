"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { SectionCard } from "@/components/shared/section-card";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatTimestampMinutes } from "@/lib/format";
import { PollOwnerSelect } from "./poll-owner-select";
import { RESULTS_TAB_ID } from "./poll-results-panel";
import type { PollAnswerData } from "./types";

/**
 * The head of the answer page: what the poll is about, whose it is and until when it is answered. A user with
 * full access chooses here whose answers are shown and finds the poll's form and results.
 */
export function PollAnswerInfo({
  data,
  onOwnerChange,
}: {
  data: PollAnswerData;
  onOwnerChange: (ownerId: number | null) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <SectionCard>
      <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm">
        {data.description && (
          <>
            <dt className="text-muted-foreground">{t("poll.description")}</dt>
            <dd className="whitespace-pre-wrap">{data.description}</dd>
          </>
        )}
        {data.location && (
          <>
            <dt className="text-muted-foreground">{t("poll.location")}</dt>
            <dd>{data.location}</dd>
          </>
        )}
        <dt className="text-muted-foreground">{t("poll.owner")}</dt>
        <dd>{data.owner?.displayName}</dd>
        <dt className="text-muted-foreground">{t("poll.deadline._")}</dt>
        <dd>{formatDate(data.deadline, ctx)}</dd>
        {data.lastUpdate && (
          <>
            <dt className="text-muted-foreground">
              {t("poll.answer.lastUpdate")}
            </dt>
            <dd>{formatTimestampMinutes(data.lastUpdate, ctx)}</dd>
          </>
        )}
      </dl>
      {data.fullAccess && (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {(data.attendees?.length ?? 0) > 0 && (
            <PollOwnerSelect data={data} onOwnerChange={onOwnerChange} />
          )}
          <Button asChild variant="ghost" size="sm">
            <Link href={`/poll/${data.pollId}`}>{t("poll.title.edit")}</Link>
          </Button>
          <Button asChild variant="ghost" size="sm">
            <Link href={`/poll/${data.pollId}?tab=${RESULTS_TAB_ID}`}>
              {t("poll.results._")}
            </Link>
          </Button>
        </div>
      )}
    </SectionCard>
  );
}
