"use client";

import { useId } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import type { PollAnswer, PollQuestion } from "./types";

/** Max length of a text answer, see PollAnswerRest.MAX_TEXT_LENGTH. */
const MAX_TEXT_LENGTH = 4000;

/** One question of the answer page: a text, one option (radio buttons) or several (checkboxes). */
export function PollQuestionControl({
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
