"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { SectionCard } from "@/components/shared/section-card";
import { fetchPollResults } from "@/lib/rs/poll";
import type { PollResults } from "./types";

/** Id of the tab, as the url carries it (`?tab=results`, the link of the mail after the end). */
export const RESULTS_TAB_ID = "results";

/**
 * All answers to a poll, for the users with full access (the endpoint refuses everybody else): per question the
 * number of answers of each option and who chose it, the texts of a text question, and who hasn't answered yet.
 */
export function PollResultsPanel({ id }: { id: number }) {
  const t = useTranslations();
  const results = useQuery({
    queryKey: ["poll", id, "results"],
    queryFn: ({ signal }) => fetchPollResults(id, signal),
  });
  if (results.isPending) {
    return <p className="p-4 text-sm text-muted-foreground">{t("loading")}</p>;
  }
  if (results.isError || !results.data) {
    return (
      <p className="p-4 text-sm text-destructive">
        {t("access.exception.noAccess")}
      </p>
    );
  }
  return <ResultsView results={results.data} />;
}

function ResultsView({ results }: { results: PollResults }) {
  const t = useTranslations();
  const attendees = results.rows.filter((row) => row.attendee);
  const answered = results.rows.filter((row) => row.answers.length > 0);
  const missing = attendees.filter((row) => row.answers.length === 0);
  const name = (row: PollResults["rows"][number]) =>
    row.attendee
      ? row.user.displayName
      : `${row.user.displayName} (${t("poll.results.notAttendee")})`;
  return (
    <div className="flex flex-col gap-4 p-4">
      <p className="text-sm" data-testid="poll-results-summary">
        {t("poll.results.answered", {
          arg0: attendees.filter((row) => row.answers.length > 0).length,
          arg1: attendees.length,
        })}
      </p>
      {results.questions.map((question) => {
        const answers = answered
          .map((row) => ({
            row,
            answer: row.answers.find((a) => a.questionUid === question.uid),
          }))
          .filter((entry) => entry.answer != null);
        return (
          <SectionCard key={question.uid}>
            <h3 className="mb-2 text-sm font-semibold">{question.text}</h3>
            {question.type === "TEXT" ? (
              <ul className="flex flex-col gap-1 text-sm">
                {answers.map(({ row, answer }) => (
                  <li key={row.user.id}>
                    <span className="font-medium">{name(row)}:</span>{" "}
                    <span className="whitespace-pre-wrap">{answer?.text}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <table className="w-full text-sm">
                <tbody>
                  {question.options.map((option) => {
                    const chosen = answers.filter(({ answer }) =>
                      answer?.optionUids.includes(option.uid ?? "")
                    );
                    return (
                      <tr key={option.uid} className="border-b last:border-0">
                        <td className="py-1 pr-4 align-top">{option.text}</td>
                        <td className="py-1 pr-4 text-right align-top font-semibold tabular-nums">
                          {chosen.length}
                        </td>
                        <td className="py-1 align-top text-muted-foreground">
                          {chosen.map(({ row }) => name(row)).join(", ")}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </SectionCard>
        );
      })}
      {missing.length > 0 && (
        <SectionCard>
          <h3 className="mb-2 text-sm font-semibold">
            {t("poll.results.missing")}
          </h3>
          <p className="text-sm">
            {missing.map((row) => row.user.displayName).join(", ")}
          </p>
        </SectionCard>
      )}
    </div>
  );
}
