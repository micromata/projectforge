"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ValueCombobox } from "@/components/shared/value-combobox";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { fetchScriptPageTargets } from "@/lib/rs/script";

const NAME = "pageTargetIds";

/**
 * The pages showing a button for the script in their top right corner (ScriptDO.pageTargets), picked
 * from what the backend offers: every list of the app and the tabs of the order statistics.
 *
 * Custom because the options are no enum of the metadata but come from `/rs/script/pageTargets`. A
 * target no longer offered (a list removed since) stays visible by its id until it is removed — the
 * backend drops it on saving anyway (`ScriptPageTargets.sanitize`).
 */
export function ScriptPageTargetsField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const targets = useQuery({
    queryKey: ["script", "pageTargets"],
    queryFn: ({ signal }) => fetchScriptPageTargets(signal),
    staleTime: Infinity,
  });
  const options = (targets.data ?? []).map((target) => ({
    value: target.id,
    label: target.title,
  }));
  const label = t("scripting.script.pageTargets._");
  return (
    <form.Field name={NAME as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        return (
          <FieldShell
            name={NAME}
            label={label}
            hint={t("scripting.script.pageTargets.info")}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <ValueCombobox
              id={ids.controlId}
              aria-label={label}
              multi
              options={options}
              selected={(field.state.value as string[] | null) ?? []}
              onChange={(next) => {
                field.handleChange(next);
                field.handleBlur();
              }}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
