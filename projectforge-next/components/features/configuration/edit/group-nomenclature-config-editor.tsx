"use client";

import { useTranslations } from "next-intl";
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { RichTextEditor } from "@/components/shared/rich-text-editor";
import {
  groupBaseName,
  GROUP_NOMENCLATURE_PLACEHOLDERS,
  parseGroupNomenclatureConfig,
  suggestedGroupName,
  type GroupNomenclatureConfig,
  type GroupNomenclatureLetterCase,
  type GroupNomenclatureRole,
} from "@/lib/group-nomenclature";
import type { JsonEditorProps } from "./json-editors";

/** The suffix inputs, by role. */
// The label keys spelled out: the i18n export finds literal keys only.
const SUFFIXES: {
  role: GroupNomenclatureRole;
  field: "managerSuffix" | "teamSuffix" | "externalSuffix";
  labelKey: string;
}[] = [
  {
    role: "managerGroup",
    field: "managerSuffix",
    labelKey: "group.nomenclature.managerSuffix",
  },
  {
    role: "team",
    field: "teamSuffix",
    labelKey: "group.nomenclature.teamSuffix",
  },
  {
    role: "externalGroup",
    field: "externalSuffix",
    labelKey: "group.nomenclature.externalSuffix",
  },
];

/** The customer and project the example names are built of. */
const EXAMPLE = {
  customer: { nummer: 123, name: "ACME AG", identifier: "ACME" },
  project: { nummer: 4, name: "Portal relaunch", identifier: "PORTAL" },
};

/**
 * The editor of the parameter `groupNomenclature`: its JSON object as one form value (`stringValue`),
 * edited through an input per setting, with the names the wizards would suggest for an example
 * customer and project. Every change writes the whole object back, so the form posts exactly what the
 * backend parses (`GroupNomenclatureConfig`); an emptied input is left out, so the default applies.
 */
export function GroupNomenclatureConfigEditor({
  label,
  hint,
  className,
}: JsonEditorProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const descriptionIds = useFieldIds();
  const patternIds = useFieldIds();
  const letterCaseIds = useFieldIds();
  const suffixIds = [useFieldIds(), useFieldIds(), useFieldIds()];
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { config, invalid: invalidJson } = parseGroupNomenclatureConfig(
          field.state.value as string | null
        );
        const update = (patch: Partial<GroupNomenclatureConfig>) => {
          const next: Record<string, unknown> = { ...config, ...patch };
          for (const key of Object.keys(next)) {
            const value = next[key];
            if (value == null || (typeof value === "string" && !value.trim())) {
              delete next[key];
            }
          }
          field.handleChange(
            Object.keys(next).length ? JSON.stringify(next) : ""
          );
        };
        const baseName = groupBaseName(config, EXAMPLE);
        const examples = SUFFIXES.map(({ role }) =>
          suggestedGroupName(
            config,
            role,
            baseName,
            role === "team" ? null : t(`task.wizard.${role}.groupNameSuffix`)
          )
        ).filter((name): name is string => !!name);
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
                <p className="text-xs text-warning">
                  {t("group.nomenclature.invalidJson")}
                </p>
              )}
              <FieldShell
                label={t("group.nomenclature.description._")}
                hint={t("group.nomenclature.description.tooltip")}
                invalid={false}
                errors={[]}
                ids={descriptionIds}
              >
                <RichTextEditor
                  id={descriptionIds.controlId}
                  value={config.description ?? null}
                  onChange={(html) => update({ description: html || null })}
                  onBlur={field.handleBlur}
                />
              </FieldShell>
              <FieldShell
                label={t("group.nomenclature.pattern._")}
                hint={t("group.nomenclature.pattern.tooltip")}
                invalid={false}
                errors={[]}
                ids={patternIds}
              >
                <Input
                  id={patternIds.controlId}
                  value={config.pattern ?? ""}
                  placeholder="{project.name}"
                  onChange={(event) => update({ pattern: event.target.value })}
                  onBlur={field.handleBlur}
                  className="max-w-md font-mono"
                />
                <p className="text-xs text-muted-foreground">
                  {t("group.nomenclature.pattern.placeholders")}:{" "}
                  <span className="font-mono">
                    {GROUP_NOMENCLATURE_PLACEHOLDERS.join(", ")}
                  </span>
                </p>
              </FieldShell>
              <div className="grid gap-4 sm:grid-cols-3">
                {SUFFIXES.map(
                  ({ role, field: suffixField, labelKey }, index) => (
                    <FieldShell
                      key={suffixField}
                      label={t(labelKey)}
                      hint={t("group.nomenclature.suffix.tooltip")}
                      invalid={false}
                      errors={[]}
                      ids={suffixIds[index]}
                    >
                      <Input
                        id={suffixIds[index].controlId}
                        value={config[suffixField] ?? ""}
                        placeholder={
                          role === "team"
                            ? undefined
                            : t(`task.wizard.${role}.groupNameSuffix`)
                        }
                        onChange={(event) =>
                          update({ [suffixField]: event.target.value })
                        }
                        onBlur={field.handleBlur}
                      />
                    </FieldShell>
                  )
                )}
              </div>
              <FieldShell
                label={t("group.nomenclature.letterCase._")}
                hint={t("group.nomenclature.letterCase.tooltip")}
                invalid={false}
                errors={[]}
                ids={letterCaseIds}
              >
                {/* "KEEP" rather than "": Radix forbids an empty item value (see SelectField). */}
                <Select
                  value={config.letterCase ?? "KEEP"}
                  onValueChange={(value) => {
                    if (value === "") return;
                    update({
                      letterCase:
                        value === "KEEP"
                          ? null
                          : (value as GroupNomenclatureLetterCase),
                    });
                  }}
                >
                  <SelectTrigger
                    id={letterCaseIds.controlId}
                    aria-labelledby={letterCaseIds.labelId}
                    className="w-fit min-w-48"
                  >
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KEEP">
                      {t("group.nomenclature.letterCase.keep")}
                    </SelectItem>
                    <SelectItem value="LOWER">
                      {t("group.nomenclature.letterCase.lower")}
                    </SelectItem>
                    <SelectItem value="UPPER">
                      {t("group.nomenclature.letterCase.upper")}
                    </SelectItem>
                  </SelectContent>
                </Select>
              </FieldShell>
              <p className="text-xs text-muted-foreground">
                {t("group.nomenclature.example._")} (
                {t("group.nomenclature.example.customer")}{" "}
                {EXAMPLE.customer.name} / {EXAMPLE.customer.identifier},{" "}
                {t("group.nomenclature.example.project")} {EXAMPLE.project.name}{" "}
                / {EXAMPLE.project.identifier}):{" "}
                <span className="font-mono">{examples.join(", ")}</span>
              </p>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
