"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { MerlinVariable } from "@/lib/rs/merlin";
import { MerlinDialogField as Field } from "./merlin-dialog-field";
import { MerlinVariableMapping } from "./merlin-variable-mapping";
import { MerlinVariableRulesFields } from "./merlin-variable-rules-fields";

/** No master variable: the value of the select's empty entry (Radix forbids ""). */
const NONE = "__none__";

/**
 * The fields of a variable, by its kind (as the legacy dialog): an input variable has a type, its rules and a
 * description (see MerlinVariableRulesFields); a dependent one maps the values of its master variable
 * (see MerlinVariableMapping).
 */
export function MerlinVariableDialogFields({
  draft,
  set,
  masterCandidates,
  setMappingValid,
}: {
  draft: MerlinVariable;
  set: (patch: Partial<MerlinVariable>) => void;
  /** The variables with allowed values this one may depend on. */
  masterCandidates: readonly MerlinVariable[];
  /** Whether every value of the mapping can be written into its csv. */
  setMappingValid: (valid: boolean) => void;
}) {
  const t = useTranslations();
  const dependent = !!draft.dependsOnName;
  const masterValues =
    masterCandidates.find((it) => it.name === draft.dependsOnName)
      ?.allowedValues ?? [];
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
        <MerlinVariableMapping
          masterName={draft.dependsOnName ?? ""}
          name={draft.name}
          masterValues={masterValues}
          csv={draft.mappingValues}
          onChange={(mappingValues, valid) => {
            set({ mappingValues });
            setMappingValid(valid);
          }}
        />
      ) : (
        <MerlinVariableRulesFields draft={draft} set={set} />
      )}
    </div>
  );
}
