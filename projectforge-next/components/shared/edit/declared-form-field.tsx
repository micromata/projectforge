"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { DatePeriodField } from "@/components/shared/form/date-period-field";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import { useJiraFieldHint } from "@/components/shared/jira/use-jira-field-hint";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import { useFormatContext } from "@/hooks/use-format";
import { cn } from "@/lib/utils";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";
import { labelKeyFor } from "@/lib/page-def/define-page";
import type { FieldDeclaration } from "@/lib/page-def/types";

const SPAN_CLASS = { 1: undefined, 2: "md:col-span-2", 3: "md:col-span-3" };

/** The key of a declaration among its siblings — stable, since it is what the field *is*. */
export function fieldKey<M extends EntityMetadata>(
  field: FieldDeclaration<M>
): string {
  if ("custom" in field) return field.custom.name;
  if ("begin" in field) return field.begin;
  if ("group" in field) return field.group.map((f) => f.name).join("+");
  return field.name;
}

/**
 * One field of a section, as its declaration describes it.
 *
 * Which component it gets follows its data type — a date goes through the shared DateInput, an enum
 * becomes a select of its own constants, a field declaring `rows` a textarea. `required`, `maxLength`
 * and the enum's values are never passed in: the field components read them from the metadata in the
 * form context, the same source the Zod schema reads.
 */
export function DeclaredFormField<M extends EntityMetadata>({
  field,
  metadata,
  /** Inside a group the width comes from the row, not from the section's grid. */
  className = cn(
    SPAN_CLASS[field.span ?? 1],
    // Placing the field in the first column is what breaks the row: the grid's auto placement moves it
    // to the next one wherever the current row has already started (see `startsRow`).
    field.startsRow && "md:col-start-1"
  ),
}: {
  field: FieldDeclaration<M>;
  metadata: M;
  className?: string;
}): ReactNode {
  const t = useTranslations();
  const format = useFormatContext();
  // Called unconditionally (a hook), so it sits above the custom/period/group returns even though only a
  // plain [DeclaredField] can carry the flag; it yields nothing unless JIRA is configured.
  const jiraHint = useJiraFieldHint(
    "jiraHint" in field ? field.jiraHint : false
  );
  // A form that is only being looked at overrides every field's own declaration — the entry is deleted
  // and offers nothing but its restore (see useFormReadOnly). The fieldset around the sections already
  // blocks the input natively; this is what makes the fields say so, down to the clear buttons a
  // dropdown offers beside itself.
  const formReadOnly = useFormReadOnly();

  if ("custom" in field) {
    const Custom = field.custom;
    return <Custom className={className} />;
  }

  const translate = t as unknown as ((key: string) => string) & {
    has: (key: string) => boolean;
  };

  if ("begin" in field) {
    // The two ends keep the labels of their own fields ("Leistungszeitraum von"/"… bis"): they name
    // the boxes for a screen reader and carry any error the backend puts on one of them, while the
    // legend above says the period once.
    const bound = (name: string) => ({
      name,
      label: translate(labelKeyFor(metadata, name, translate.has)),
    });
    return (
      <DatePeriodField
        label={translate(field.periodLabelKey)}
        begin={bound(field.begin)}
        end={bound(field.end)}
        hint={field.hintKey ? translate(field.hintKey) : undefined}
        disabled={formReadOnly}
        periodKinds={field.periodKinds}
        paging={field.paging}
        longLabel={field.longLabel}
        className={className}
      />
    );
  }

  if ("group" in field) {
    return (
      <div
        // Wrapping rather than a container query: every member is bounded to the width of its own
        // value (a digit count, a date's ten characters), so they fit side by side wherever that
        // width is there and drop to the next line only where it genuinely isn't.
        className={cn(
          "flex min-w-0 flex-wrap items-start gap-x-4 gap-y-4",
          className
        )}
      >
        {field.group.map((member) => (
          <DeclaredFormField
            key={member.name}
            field={member}
            metadata={metadata}
            // `w-auto` against the `w-full` every field carries ([FieldShell]'s `Field`): in a row
            // that would make each member take the whole cell and push the next one onto a line of
            // its own. A field bounded to a digit count is as wide as its box (see NumberField's
            // `maxDigits`) and stays that way; the others share what is left.
            className={cn(
              "min-w-0",
              member.maxDigits ? "w-auto shrink-0" : "flex-1 basis-40"
            )}
          />
        ))}
      </div>
    );
  }

  const meta: FieldMetadata = metadata.fields[field.name] ?? {
    dataType: "STRING",
    required: false,
  };
  const label = translate(
    labelKeyFor(metadata, field.name, translate.has, field.labelKey)
  );
  // The field's own hint wins; a JIRA field falls back to the "supports JIRA" hint, which is present
  // only where JIRA is configured (see useJiraFieldHint).
  const hint =
    (field.hintKey ? translate(field.hintKey) : undefined) ?? jiraHint;
  const common = { name: field.name, label, hint, className };
  const readOnly = field.readOnly || formReadOnly;

  if (meta.enumValues) {
    return (
      <SelectField
        {...common}
        disabled={readOnly}
        emphasized={field.emphasized}
        options={meta.enumValues.map((v) => ({
          value: v.value,
          label: v.i18nKey ? translate(v.i18nKey) : v.value,
        }))}
      />
    );
  }
  if (field.rows) {
    return <TextAreaField {...common} rows={field.rows} disabled={readOnly} />;
  }
  if (meta.dataType === "BOOLEAN") {
    return <CheckboxField {...common} disabled={readOnly} />;
  }
  if (
    meta.dataType === "AMOUNT" ||
    meta.dataType === "DECIMAL" ||
    meta.dataType === "INT"
  ) {
    // A number is held as one, so it needs the number box even where it has no decimals: a plain text
    // input would put the string "50" into a field the schema declares as `z.number()`, which fails
    // validation on a value the user typed correctly.
    return (
      <NumberField
        {...common}
        disabled={readOnly}
        fractionDigits={meta.dataType === "INT" ? 0 : undefined}
        maxDigits={field.maxDigits}
        align={field.alignNumber}
        // The currency behind the box comes from the user's settings, never from a text here.
        suffix={meta.dataType === "AMOUNT" ? format.currency : undefined}
      />
    );
  }
  if (meta.dataType === "TASK") {
    // The tree, not an autocomplete: a task is picked by where it sits, and its title alone is
    // ambiguous — several projects have a „Wartung". The same picker every other reference to a task
    // uses (a timesheet, an order position).
    return <TaskSelectField {...common} disabled={readOnly} />;
  }
  const searchEntity = SEARCH_ENTITY[meta.dataType];
  if (searchEntity) {
    return <EntityAutocompleteField {...common} entity={searchEntity} />;
  }
  return (
    <InputField
      {...common}
      type={meta.dataType === "DATE" ? "date" : "text"}
      disabled={readOnly}
      emphasized={field.emphasized}
    />
  );
}

/**
 * The REST category to search in for a field referencing another entity — `{category}/autosearch`, the
 * same lookup the legacy pages use (`AutoCompletion.getAutoCompletion4Users` and its siblings).
 *
 * Only the types that have such an endpoint; a data type missing here falls through to a text input,
 * which is visible enough to be fixed rather than silently rendering nothing.
 *
 * `TASK` is deliberately absent: a task is picked from the tree, above.
 */
const SEARCH_ENTITY: Partial<Record<FieldMetadata["dataType"], string>> = {
  USER: "user",
  GROUP: "group",
  EMPLOYEE: "employee",
  COST1: "cost1",
  COST2: "cost2",
  KONTO: "account",
};
