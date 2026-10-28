"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { InputField } from "@/components/shared/form/input-field";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import { useFieldArray } from "@/hooks/use-field-array";
import { PollRowButton } from "./poll-row-button";
import type { OptionRow } from "./types";

/** The options of a choice question, one input each. */
export function PollQuestionOptions({ name }: { name: string }) {
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
            <PollRowButton
              icon={Cancel01Icon}
              label={t("poll.option.remove")}
              onClick={() => options.remove(index)}
            />
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
