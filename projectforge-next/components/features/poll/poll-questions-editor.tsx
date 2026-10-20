"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Cancel01Icon,
  Delete02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import { useFieldArray } from "@/hooks/use-field-array";
import { fetchPollTemplateQuestions, fetchPollTemplates } from "@/lib/rs/poll";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import type { PollOption, PollQuestion, PollQuestionType } from "./types";

/** The types of a question with their labels, spelled out so the i18n export finds the keys. */
const QUESTION_TYPES: { type: PollQuestionType; labelKey: string }[] = [
  { type: "SINGLE_CHOICE", labelKey: "poll.questionType.SINGLE_CHOICE" },
  { type: "MULTIPLE_CHOICE", labelKey: "poll.questionType.MULTIPLE_CHOICE" },
  { type: "TEXT", labelKey: "poll.questionType.TEXT" },
];

/** Rows of the field array: a question or an option has no id of its own, only a uid (or none yet). */
type QuestionRow = PollQuestion & { id?: null; deleted?: boolean };
type OptionRow = PollOption & { id?: null; deleted?: boolean };

/**
 * The questions of a poll: text, type and — of a choice question — the options, in the order they are asked.
 * Questions can be added, removed and moved, or taken from a template (a built-in one or a poll marked as
 * template, see PollEntityRest.getTemplates).
 *
 * The uids of the questions and options are the backend's: a new one is posted without, and gets one on save
 * (PollEntityRest.onBeforeSaveOrUpdate). Answers name them, so a question keeps its uid when it is edited.
 */
export function PollQuestionsEditor({ className }: { className?: string }) {
  const t = useTranslations();
  const readOnly = useFormReadOnly();
  const questions = useFieldArray<QuestionRow>("questions");
  const typeOptions = QUESTION_TYPES.map(({ type, labelKey }) => ({
    value: type,
    label: t(labelKey),
  }));
  return (
    <div className={cn("flex flex-col gap-4", className)}>
      {questions.visible.length === 0 && (
        <p className="py-2 text-xs text-muted-foreground">
          {t("poll.error.oneQuestionRequired")}
        </p>
      )}
      {questions.visible.map(([question, index]) => (
        <div
          key={index}
          className="flex flex-col gap-3 rounded-md border p-3"
          data-testid="poll-question"
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-[minmax(0,3fr)_minmax(0,1fr)_auto]">
            <InputField
              name={questions.fieldName(index, "text")}
              label={`${t("poll.question._")} ${index + 1}`}
              metadataLess
            />
            <SelectField
              name={questions.fieldName(index, "type")}
              label={t("poll.questionType._")}
              options={typeOptions}
              clearable={false}
              metadataLess
            />
            {!readOnly && (
              <div className="flex items-end gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={t("poll.question.moveUp")}
                  title={t("poll.question.moveUp")}
                  disabled={index === 0}
                  onClick={() => questions.move(index, index - 1)}
                >
                  <HugeiconsIcon icon={ArrowUp01Icon} aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={t("poll.question.moveDown")}
                  title={t("poll.question.moveDown")}
                  disabled={index === questions.rows.length - 1}
                  onClick={() => questions.move(index, index + 1)}
                >
                  <HugeiconsIcon icon={ArrowDown01Icon} aria-hidden />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={t("poll.question.remove")}
                  title={t("poll.question.remove")}
                  onClick={() => questions.remove(index)}
                >
                  <HugeiconsIcon icon={Delete02Icon} aria-hidden />
                </Button>
              </div>
            )}
          </div>
          {question.type !== "TEXT" && (
            <QuestionOptions name={questions.fieldName(index, "options")} />
          )}
        </div>
      ))}
      {!readOnly && (
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() =>
              questions.add({
                text: "",
                type: "SINGLE_CHOICE",
                options: [{ text: "" }, { text: "" }],
              })
            }
          >
            <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
            {t("poll.question.add")}
          </Button>
          <TemplateSelect
            onQuestions={(taken) => taken.forEach((q) => questions.add(q))}
          />
        </div>
      )}
    </div>
  );
}

/** The options of a choice question, one input each. */
function QuestionOptions({ name }: { name: string }) {
  const t = useTranslations();
  const readOnly = useFormReadOnly();
  const options = useFieldArray<OptionRow>(name);
  return (
    <div className="flex flex-col gap-2 pl-4">
      {options.visible.map(([, index]) => (
        <div key={index} className="flex items-end gap-1">
          <InputField
            name={options.fieldName(index, "text")}
            label={`${t("poll.option._")} ${index + 1}`}
            className="flex-1"
            metadataLess
          />
          {!readOnly && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={t("poll.option.remove")}
              title={t("poll.option.remove")}
              onClick={() => options.remove(index)}
            >
              <HugeiconsIcon icon={Cancel01Icon} aria-hidden />
            </Button>
          )}
        </div>
      ))}
      {!readOnly && (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="self-start"
          onClick={() => options.add({ text: "" })}
        >
          <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
          {t("poll.option.add")}
        </Button>
      )}
    </div>
  );
}

/** Takes the questions of a template into the poll (appended, with new uids). */
function TemplateSelect({
  onQuestions,
}: {
  onQuestions: (questions: QuestionRow[]) => void;
}) {
  const t = useTranslations();
  // Remounts the select after each choice, so the same template can be taken twice.
  const [round, setRound] = useState(0);
  const templates = useQuery({
    queryKey: ["poll", "templates"],
    queryFn: ({ signal }) => fetchPollTemplates(signal),
  });
  const list = templates.data ?? [];
  if (list.length === 0) return null;
  return (
    <Select
      key={round}
      onValueChange={(value) => {
        const template = list[Number(value)];
        setRound((r) => r + 1);
        if (!template) return;
        fetchPollTemplateQuestions(template)
          .then((questions) => {
            onQuestions(questions);
            toast.success(t("poll.questionTemplate.taken"));
          })
          .catch((error: unknown) =>
            toast.error(error instanceof Error ? error.message : String(error))
          );
      }}
    >
      <SelectTrigger
        size="sm"
        className="w-auto"
        aria-label={t("poll.questionTemplate._")}
      >
        <SelectValue placeholder={t("poll.questionTemplate._")} />
      </SelectTrigger>
      <SelectContent>
        {list.map((template, index) => (
          <SelectItem key={index} value={String(index)}>
            {template.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
