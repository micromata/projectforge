"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { fetchFeedbackData } from "@/lib/rs/feedback";
import { FeedbackForm } from "./feedback-form";

/**
 * The "Send feedback" page ("Feedback senden", `/next/feedback`), successor of Wicket's `wa/feedback`.
 * A standalone, non-entity form: it mails a free-text feedback to the configured address through the
 * reused SendFeedback service. The Pacman button of the old Wicket form is dropped.
 *
 * The form is rendered only once the initial data (the read-only receiver and sender) has arrived, so it
 * can seed its own state.
 */
export function FeedbackPage() {
  const t = useTranslations();

  const data = useQuery({
    queryKey: ["feedback"],
    queryFn: ({ signal }) => fetchFeedbackData(signal),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.gear.feedback")}
        title={t("feedback.send.title")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        {data.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {data.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data.data && <FeedbackForm initial={data.data} />}
      </div>
    </PageShell>
  );
}
