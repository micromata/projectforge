"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Delete02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import { useFieldArray } from "@/hooks/use-field-array";
import { cn } from "@/lib/utils";
import { PollQuestionOptions } from "./poll-question-options";
import { PollRowButton } from "./poll-row-button";
import { PollTemplateSelect } from "./poll-template-select";
import type { PollQuestionType, QuestionRow } from "./types";

/** The types of a question with their labels, spelled out so the i18n export finds the keys. */
const QUESTION_TYPES: { type: PollQuestionType; labelKey: string }[] = [
  { type: "SINGLE_CHOICE", labelKey: "poll.questionType.SINGLE_CHOICE" },
  { type: "MULTIPLE_CHOICE", labelKey: "poll.questionType.MULTIPLE_CHOICE" },
  { type: "TEXT", labelKey: "poll.questionType.TEXT" },
];

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
                <PollRowButton
                  icon={ArrowUp01Icon}
                  label={t("poll.question.moveUp")}
                  disabled={index === 0}
                  onClick={() => questions.move(index, index - 1)}
                />
                <PollRowButton
                  icon={ArrowDown01Icon}
                  label={t("poll.question.moveDown")}
                  disabled={index === questions.rows.length - 1}
                  onClick={() => questions.move(index, index + 1)}
                />
                <PollRowButton
                  icon={Delete02Icon}
                  label={t("poll.question.remove")}
                  onClick={() => questions.remove(index)}
                />
              </div>
            )}
          </div>
          {question.type !== "TEXT" && (
            <PollQuestionOptions name={questions.fieldName(index, "options")} />
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
          <PollTemplateSelect
            onQuestions={(taken) => taken.forEach((q) => questions.add(q))}
          />
        </div>
      )}
    </div>
  );
}
