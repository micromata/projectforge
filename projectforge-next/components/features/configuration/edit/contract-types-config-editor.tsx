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
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import {
  EMPTY_CONTRACT_TYPE,
  parseContractTypesConfig,
  serializeContractTypesConfig,
  type ContractTypeEntry,
} from "../contract-types-config";
import { ColumnHeader } from "./column-header";
import type { JsonEditorProps } from "./json-editors";

// Fixed width of the actions (3 icon buttons of 1.75rem): the header row has no buttons, so an auto column
// would shift its headers against the inputs.
const COLUMNS = "grid grid-cols-[10rem_minmax(12rem,1fr)_5.25rem] items-center gap-2";

/**
 * The editor of the parameter `orga.contractTypes`: one row per type (value and label) in the order of
 * the selection in the contracts. Every change writes the whole array back; the backend's errors arrive on
 * `stringValue` and are listed above the rows.
 */
export function ContractTypesConfigEditor({ label, hint, className }: JsonEditorProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { entries, invalid: invalidJson } = parseContractTypesConfig(
          field.state.value as string | null
        );
        const write = (next: ContractTypeEntry[]) =>
          field.handleChange(serializeContractTypesConfig(next));
        const set = (index: number, patch: Partial<ContractTypeEntry>) =>
          write(entries.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
        const move = (index: number, delta: number) => {
          const target = index + delta;
          if (target < 0 || target >= entries.length) return;
          const next = [...entries];
          [next[index], next[target]] = [next[target], next[index]];
          write(next);
        };
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
              className="grid gap-3 rounded-md border p-3"
            >
              {invalid && errors.length > 0 && (
                <div className="grid gap-1">
                  {errors.map((error) => (
                    <FieldError key={error}>{error}</FieldError>
                  ))}
                </div>
              )}
              {invalidJson && (
                <p className="text-xs text-warning">
                  {t("administration.configuration.contractTypes.invalidJson")}
                </p>
              )}
              <div className="grid max-w-3xl gap-2">
                <div
                  className={`${COLUMNS} text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground`}
                >
                  <ColumnHeader
                    label={t("administration.configuration.contractTypes.value")}
                    hint={t("administration.configuration.contractTypes.help.value")}
                  />
                  <ColumnHeader
                    label={t("administration.configuration.contractTypes.label")}
                    hint={t("administration.configuration.contractTypes.help.label")}
                  />
                  <span />
                </div>
                {entries.map((entry, index) => {
                  const rowLabel = entry.label || entry.value || `${index + 1}`;
                  return (
                    <div
                      // The rows have no identity of their own; the index is what the JSON array has.
                      key={index}
                      className={COLUMNS}
                    >
                      <Input
                        value={entry.value}
                        placeholder="NDA"
                        aria-label={`${t("administration.configuration.contractTypes.value")}: ${rowLabel}`}
                        onChange={(e) => set(index, { value: e.target.value })}
                        onBlur={field.handleBlur}
                      />
                      <Input
                        value={entry.label}
                        aria-label={`${t("administration.configuration.contractTypes.label")}: ${rowLabel}`}
                        onChange={(e) => set(index, { label: e.target.value })}
                        onBlur={field.handleBlur}
                      />
                      <div className="flex">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={index === 0}
                          aria-label={t("administration.configuration.contractTypes.moveUp", { arg0: rowLabel })}
                          onClick={() => move(index, -1)}
                        >
                          <HugeiconsIcon icon={ArrowUp01Icon} className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={index === entries.length - 1}
                          aria-label={t("administration.configuration.contractTypes.moveDown", { arg0: rowLabel })}
                          onClick={() => move(index, 1)}
                        >
                          <HugeiconsIcon icon={ArrowDown01Icon} className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          aria-label={t("administration.configuration.contractTypes.remove", { arg0: rowLabel })}
                          onClick={() => write(entries.filter((_, i) => i !== index))}
                        >
                          <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
              <div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => write([...entries, EMPTY_CONTRACT_TYPE])}
                >
                  <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
                  {t("administration.configuration.contractTypes.add")}
                </Button>
              </div>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
