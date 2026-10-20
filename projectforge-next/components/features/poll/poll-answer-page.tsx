"use client";

import { useId, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatTimestampMinutes } from "@/lib/format";
import { fetchPollAnswers, savePollAnswers } from "@/lib/rs/poll";
import { toast } from "@/lib/toast";
import { RESULTS_TAB_ID } from "./poll-results-panel";
import type { PollAnswer, PollAnswerData, PollQuestion } from "./types";

/** Max length of a text answer, see PollAnswerRest.MAX_TEXT_LENGTH. */
const MAX_TEXT_LENGTH = 4000;

/**
 * Answering a poll (`/poll/<id>/answer`, the link of the invitation mail): the questions, one control each — a
 * text, one option (radio buttons) or several (checkboxes). The answers can be changed until the poll is
 * finished; afterwards they are shown only.
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
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        {data.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {data.isError && (
          <p className="text-sm text-destructive">
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
      </div>
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
  const ctx = useFormatContext();
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
    <div className="flex flex-col gap-4">
      {readOnly && (
        <FormAlert tone="info">{t("poll.answer.notOpen")}</FormAlert>
      )}
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
              <OwnerSelect data={data} onOwnerChange={onOwnerChange} />
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
      {data.questions.map((question, index) => (
        <SectionCard key={question.uid} data-testid="poll-answer-question">
          <QuestionControl
            index={index}
            question={question}
            answer={question.uid ? answers[question.uid] : undefined}
            readOnly={readOnly}
            onChange={(change) => question.uid && update(question.uid, change)}
          />
        </SectionCard>
      ))}
      {!readOnly && (
        <div>
          <Button
            type="button"
            disabled={save.isPending}
            onClick={() => save.mutate()}
          >
            {t("poll.answer.save")}
          </Button>
        </div>
      )}
    </div>
  );
}

/** Whose answers are shown: the logged-in user's, or those of an attendee (full access only). */
function OwnerSelect({
  data,
  onOwnerChange,
}: {
  data: PollAnswerData;
  onOwnerChange: (ownerId: number | null) => void;
}) {
  const t = useTranslations();
  const id = useId();
  const me = useCurrentUserRef();
  return (
    <div className="flex items-center gap-2">
      <Label htmlFor={id} className="text-sm">
        {t("poll.answer.for")}
      </Label>
      <Select
        value={String(data.answerOwner.id)}
        onValueChange={(value) =>
          onOwnerChange(Number(value) === me?.id ? null : Number(value))
        }
      >
        <SelectTrigger id={id} size="sm" className="w-auto min-w-48">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {/* The logged-in user may not be an attendee, but answers for themselves anyway. */}
          {me && !data.attendees?.some((a) => a.id === me.id) && (
            <SelectItem value={String(me.id)}>{me.displayName}</SelectItem>
          )}
          {data.attendees?.map((attendee) => (
            <SelectItem key={attendee.id} value={String(attendee.id)}>
              {attendee.displayName}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

function QuestionControl({
  index,
  question,
  answer,
  readOnly,
  onChange,
}: {
  index: number;
  question: PollQuestion;
  answer: PollAnswer | undefined;
  readOnly: boolean;
  onChange: (change: Partial<PollAnswer>) => void;
}) {
  const id = useId();
  const heading = (
    <h3 id={`${id}-q`} className="mb-3 text-sm font-semibold">
      {`${index + 1}. ${question.text ?? ""}`}
    </h3>
  );
  if (question.type === "TEXT") {
    return (
      <>
        {heading}
        <Textarea
          aria-labelledby={`${id}-q`}
          rows={3}
          maxLength={MAX_TEXT_LENGTH}
          disabled={readOnly}
          value={answer?.text ?? ""}
          onChange={(e) => onChange({ text: e.target.value })}
        />
      </>
    );
  }
  if (question.type === "SINGLE_CHOICE") {
    return (
      <>
        {heading}
        <RadioGroup
          aria-labelledby={`${id}-q`}
          disabled={readOnly}
          value={answer?.optionUids[0] ?? ""}
          onValueChange={(value) => onChange({ optionUids: [value] })}
          className="gap-2"
        >
          {question.options.map((option, optionIndex) => (
            <div key={option.uid} className="flex items-center gap-2">
              <RadioGroupItem
                id={`${id}-${optionIndex}`}
                value={option.uid ?? ""}
              />
              <Label
                htmlFor={`${id}-${optionIndex}`}
                className="text-sm font-normal"
              >
                {option.text}
              </Label>
            </div>
          ))}
        </RadioGroup>
      </>
    );
  }
  const chosen = answer?.optionUids ?? [];
  return (
    <>
      {heading}
      <div
        role="group"
        aria-labelledby={`${id}-q`}
        className="flex flex-col gap-2"
      >
        {question.options.map((option, optionIndex) => (
          <div key={option.uid} className="flex items-center gap-2">
            <Checkbox
              id={`${id}-${optionIndex}`}
              disabled={readOnly}
              checked={chosen.includes(option.uid ?? "")}
              onCheckedChange={(checked) =>
                onChange({
                  optionUids:
                    checked === true
                      ? [...chosen, option.uid ?? ""]
                      : chosen.filter((uid) => uid !== option.uid),
                })
              }
            />
            <Label
              htmlFor={`${id}-${optionIndex}`}
              className="text-sm font-normal"
            >
              {option.text}
            </Label>
          </div>
        ))}
      </div>
    </>
  );
}

function toMap(answers: PollAnswer[]): Record<string, PollAnswer> {
  return Object.fromEntries(
    answers.map((answer) => [answer.questionUid, answer])
  );
}
