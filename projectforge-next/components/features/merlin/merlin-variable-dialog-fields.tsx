"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import type { MerlinVariable } from "@/lib/rs/merlin";
import { MerlinDialogField as Field } from "./merlin-dialog-field";
import { MerlinVariableRulesFields } from "./merlin-variable-rules-fields";

/** No master variable: the value of the select's empty entry (Radix forbids ""). */
const NONE = "__none__";

/**
 * The fields of a variable, by its kind (as the legacy dialog): an input variable has a type, its rules and a
 * description (see MerlinVariableRulesFields); a dependent one maps the values of its master variable.
 */
export function MerlinVariableDialogFields({
  draft,
  set,
  masterCandidates,
}: {
  draft: MerlinVariable;
  set: (patch: Partial<MerlinVariable>) => void;
  /** The variables with allowed values this one may depend on. */
  masterCandidates: readonly MerlinVariable[];
}) {
  const t = useTranslations();
  const dependent = !!draft.dependsOnName;
  const masterValues = masterCandidates
    .find((it) => it.name === draft.dependsOnName)
    ?.allowedValues?.join(", ");
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Field label={t("plugins.merlin.variable.name")}>
        {(id) => (
          <Input
            id={id}
            value={draft.name}
            onChange={(e) => set({ name: e.target.value })}
          />
        )}
      </Field>
      {!draft.masterVariable && (
        <Field label={t("plugins.merlin.variable.dependsOn")}>
          {(id) => (
            <Select
              value={draft.dependsOnName ?? NONE}
              onValueChange={(it) =>
                set({ dependsOnName: it === NONE ? null : it })
              }
            >
              <SelectTrigger id={id} className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>–</SelectItem>
                {masterCandidates.map((it) => (
                  <SelectItem key={it.name} value={it.name}>
                    {it.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </Field>
      )}
      {dependent ? (
        <>
          <ReadOnlyValue
            label={t("plugins.merlin.variable.master.values")}
            className="sm:col-span-2"
          >
            {masterValues}
          </ReadOnlyValue>
          <Field
            label={t("plugins.merlin.variable.mapping._")}
            hint={t("plugins.merlin.variable.mapping.info")}
          >
            {(id) => (
              <Textarea
                id={id}
                value={draft.mappingValues ?? ""}
                onChange={(e) => set({ mappingValues: e.target.value || null })}
              />
            )}
          </Field>
        </>
      ) : (
        <MerlinVariableRulesFields draft={draft} set={set} />
      )}
    </div>
  );
}
