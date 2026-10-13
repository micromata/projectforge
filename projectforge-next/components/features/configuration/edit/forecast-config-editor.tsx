"use client";

import { useTranslations } from "next-intl";
import { FieldError } from "@/components/ui/field";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { RichTextEditor } from "@/components/shared/rich-text-editor";
import { parseForecastConfig, type ForecastConfig } from "../forecast-config";
import type { JsonEditorProps } from "./json-editors";

/**
 * The editor of the parameter `fibu.forecast`: its JSON object as one form value (`stringValue`),
 * edited through an input per setting. Every change writes the whole object back, so the form posts
 * exactly what the backend parses (`ForecastConfig`).
 */
export function ForecastConfigEditor({
  label,
  hint,
  className,
}: JsonEditorProps) {
  const t = useTranslations("fibu.auftrag.forecast.config");
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const hintIds = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { config, invalid: invalidJson } = parseForecastConfig(
          field.state.value as string | null
        );
        const update = (patch: Partial<ForecastConfig>) =>
          field.handleChange(JSON.stringify({ ...config, ...patch }));
        return (
          <FieldShell
            name="stringValue"
            label={label}
            hint={hint}
            invalid={invalid}
            errors={[]}
            className={className}
            ids={ids}
          >
            <div
              id={ids.controlId}
              role="group"
              aria-labelledby={ids.labelId}
              className="grid gap-4 rounded-md border p-3"
            >
              {invalid && errors.length > 0 && (
                <div className="grid gap-1">
                  {errors.map((error) => (
                    <FieldError key={error}>{error}</FieldError>
                  ))}
                </div>
              )}
              {invalidJson && (
                <p className="text-xs text-warning">{t("invalidJson")}</p>
              )}
              <FieldShell
                label={t("planningDateHint._")}
                hint={t("planningDateHint.tooltip")}
                invalid={false}
                errors={[]}
                ids={hintIds}
              >
                <RichTextEditor
                  id={hintIds.controlId}
                  value={config.planningDateHint}
                  onChange={(html) =>
                    update({ planningDateHint: html || null })
                  }
                  onBlur={field.handleBlur}
                />
              </FieldShell>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
