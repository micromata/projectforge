"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { RichTextField } from "@/components/shared/form/rich-text-field";
import { SelectField } from "@/components/shared/form/select-field";
import { SCRIPT_METADATA } from "@/lib/metadata/script.generated";
import { SCRIPT_PARAMETER_KEYS } from "@/lib/rs/script";
import { cn } from "@/lib/utils";
import { fromMetadata } from "@/lib/validation/from-metadata";

const m = fromMetadata(SCRIPT_METADATA);

/**
 * The six parameters of a script, one block each: name and type, the description (rich text) below. The values are entered on
 * the execution page; here only what it asks for is declared.
 *
 * A custom field because the DTO nests a parameter's three columns into one object (`parameter1.name`),
 * while the metadata knows them as `parameter1Name` & co. — the labels and rules are taken from there.
 */
export function ScriptParametersField({ className }: { className?: string }) {
  const t = useTranslations();
  // The constants are the same for all six parameters.
  const typeOptions = m.enumOptions("parameter1Type", t);
  return (
    <div className={cn("flex flex-col gap-5", className)}>
      {SCRIPT_PARAMETER_KEYS.map((key, index) => (
        <div key={key} className="flex flex-col gap-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <InputField
              name={`${key}.name`}
              label={`${t("scripting.script.parameterName")} ${index + 1}`}
              metadataLess
            />
            <SelectField
              name={`${key}.type`}
              label={t("scripting.script.parameterType._")}
              options={typeOptions}
              clearable
              metadataLess
            />
          </div>
          <RichTextField
            name={`${key}.description`}
            label={t("description")}
            metadataLess
          />
        </div>
      ))}
    </div>
  );
}
