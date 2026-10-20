"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { fetchPollAnswers, savePollAnswers } from "@/lib/rs/poll";
import { toast } from "@/lib/toast";
import { PollAnswerInfo } from "./poll-answer-info";
import { PollQuestionControl } from "./poll-question-control";
import type { PollAnswer, PollAnswerData } from "./types";

/**
 * Answering a poll (`/poll/<id>/answer`, the link of the invitation mail): the questions, one control each (see
 * PollQuestionControl). The answers can be changed until the poll is finished; afterwards they are shown only.
 *
 * A user with full access answers for an attendee, too (who gets a mail about it), and finds the poll's form
 * and results from here.
 */
export function PollAnswerPage({ pollId }: { pollId: number }) {
  const t = useTranslations();
  const [ownerId, setOwnerId] = useState<number | null>(null);
  const queryKey = ["poll", pollId, "answers", ownerId];
  const data = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchPollAnswers(pollId, ownerId, signal),
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("poll.title.list")}
        title={data.data?.title ?? t("poll.answer.title")}
      />
      {data.isPending && (
        <p className="px-4 pt-2 text-sm text-muted-foreground">
          {t("loading")}
        </p>
      )}
      {data.isError && (
        <p className="px-4 pt-2 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      )}
      {data.data && (
        <AnswerForm
          // A new form for each owner: the answers are theirs.
          key={data.data.answerOwner.id}
          data={data.data}
          queryKey={queryKey}
          onOwnerChange={setOwnerId}
        />
      )}
    </PageShell>
  );
}

function AnswerForm({
  data,
  queryKey,
  onOwnerChange,
}: {
  data: PollAnswerData;
  queryKey: unknown[];
  onOwnerChange: (ownerId: number | null) => void;
}) {
  const t = useTranslations();
  const qc = useQueryClient();
  const [answers, setAnswers] = useState<Record<string, PollAnswer>>(() =>
    toMap(data.answers)
  );
  const save = useMutation({
    mutationFn: () =>
      savePollAnswers(
        data.pollId,
        data.fullAccess ? data.answerOwner.id : null,
        Object.values(answers)
      ),
    onSuccess: (saved) => {
      qc.setQueryData(queryKey, saved);
      toast.success(t("poll.answer.saved"));
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const update = (questionUid: string, change: Partial<PollAnswer>) =>
    setAnswers((previous) => ({
      ...previous,
      [questionUid]: {
        questionUid,
        text: change.text ?? previous[questionUid]?.text ?? null,
        optionUids:
          change.optionUids ?? previous[questionUid]?.optionUids ?? [],
      },
    }));
  const readOnly = !data.open;
  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
      <div className="flex flex-1 flex-col gap-4 overflow-y-auto px-4 pb-6 pt-2">
        {readOnly && (
          <FormAlert tone="info">{t("poll.answer.notOpen")}</FormAlert>
        )}
        <PollAnswerInfo data={data} onOwnerChange={onOwnerChange} />
        {data.questions.map((question, index) => (
          <SectionCard key={question.uid} data-testid="poll-answer-question">
            <PollQuestionControl
              index={index}
              question={question}
              answer={question.uid ? answers[question.uid] : undefined}
              readOnly={readOnly}
              onChange={(change) =>
                question.uid && update(question.uid, change)
              }
            />
          </SectionCard>
        ))}
      </div>
      <FormActionBar>
        <Button asChild variant="outline">
          <Link href="/poll">{readOnly ? t("back") : t("cancel")}</Link>
        </Button>
        {!readOnly && (
          <Button
            type="button"
            disabled={save.isPending}
            onClick={() => save.mutate()}
          >
            {t("poll.answer.save")}
          </Button>
        )}
      </FormActionBar>
    </div>
  );
}

function toMap(answers: PollAnswer[]): Record<string, PollAnswer> {
  return Object.fromEntries(
    answers.map((answer) => [answer.questionUid, answer])
  );
}
