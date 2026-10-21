"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TagInput } from "@/components/shared/tag-input";
import { NumberBox } from "@/components/shared/form/number-box";
import {
  MERLIN_VARIABLE_TYPES,
  type MerlinVariable,
  type MerlinVariableType,
} from "@/lib/rs/merlin";
import { MerlinDialogField as Field } from "./merlin-dialog-field";
import { MERLIN_VARIABLE_TYPE_KEYS } from "./merlin-variable-types";

const toNumber = (value: unknown) =>
  value == null || value === "" ? null : Number(value);

/**
 * The fields of an input variable: its type and the rules of its values (mandatory, unique, allowed values,
 * bounds), its sort name and its description.
 */
export function MerlinVariableRulesFields({
  draft,
  set,
}: {
  draft: MerlinVariable;
  set: (patch: Partial<MerlinVariable>) => void;
}) {
  const t = useTranslations();
  const type = draft.type ?? "STRING";
  return (
    <>
      <Field label={t("plugins.merlin.variable.type._")}>
        {(id) => (
          <Select
            value={type}
            onValueChange={(it) => set({ type: it as MerlinVariableType })}
          >
            <SelectTrigger id={id} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {MERLIN_VARIABLE_TYPES.map((it) => (
                <SelectItem key={it} value={it}>
                  {t(MERLIN_VARIABLE_TYPE_KEYS[it])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </Field>
      <Field
        label={t("plugins.merlin.variable.sortName._")}
        hint={t("plugins.merlin.variable.sortName.info")}
      >
        {(id) => (
          <Input
            id={id}
            value={draft.sortName ?? ""}
            onChange={(e) => set({ sortName: e.target.value || null })}
          />
        )}
      </Field>
      <div className="flex items-center gap-6 sm:col-span-2">
        <label className="flex items-center gap-2 text-sm">
          <Checkbox
            checked={!!draft.required}
            onCheckedChange={(it) => set({ required: it === true })}
          />
          {t("plugins.merlin.variable.required")}
        </label>
        <label className="flex items-center gap-2 text-sm">
          <Checkbox
            checked={!!draft.unique}
            onCheckedChange={(it) => set({ unique: it === true })}
          />
          {t("plugins.merlin.variable.unique")}
        </label>
      </div>
      {type === "STRING" && (
        <div className="sm:col-span-2">
          <Field
            label={t("plugins.merlin.variable.allowedValues._")}
            hint={t("plugins.merlin.variable.allowedValues.info")}
          >
            {() => (
              <TagInput
                value={draft.allowedValues ?? []}
                onChange={(values) =>
                  set({ allowedValues: values.length ? values : null })
                }
                inputAriaLabel={t("plugins.merlin.variable.allowedValues._")}
                commitOnComma={false}
              />
            )}
          </Field>
        </div>
      )}
      {(type === "INT" || type === "FLOAT") && (
        <>
          <Field label={t("plugins.merlin.variable.minValue")}>
            {(id) => (
              <NumberBox
                id={id}
                value={toNumber(draft.minimumValue)}
                onChange={(it) => set({ minimumValue: it })}
                fractionDigits={type === "INT" ? 0 : undefined}
              />
            )}
          </Field>
          <Field label={t("plugins.merlin.variable.maxValue")}>
            {(id) => (
              <NumberBox
                id={id}
                value={toNumber(draft.maximumValue)}
                onChange={(it) => set({ maximumValue: it })}
                fractionDigits={type === "INT" ? 0 : undefined}
              />
            )}
          </Field>
        </>
      )}
      {type === "FLOAT" && (
        <Field
          label={t("plugins.merlin.variable.scale._")}
          hint={t("plugins.merlin.variable.scale.info")}
        >
          {(id) => (
            <NumberBox
              id={id}
              value={draft.scale ?? null}
              onChange={(it) => set({ scale: it })}
              fractionDigits={0}
            />
          )}
        </Field>
      )}
      <div className="sm:col-span-2">
        <Field label={t("description")}>
          {(id) => (
            <Textarea
              id={id}
              value={draft.description ?? ""}
              onChange={(e) => set({ description: e.target.value || null })}
            />
          )}
        </Field>
      </div>
    </>
  );
}
