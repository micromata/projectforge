"use client";

import { useId, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useBookEditForm } from "./book-edit-context";
import { REQUIRED, type BookEditValues } from "./book-edit-schema";

type Path = keyof BookEditValues;

interface BaseProps {
  name: Path;
  label: string;
  required?: boolean;
  hint?: string;
  className?: string;
}

interface InputFieldProps extends BaseProps {
  type?: "text" | "date";
  placeholder?: string;
}

/**
 * Turns the errors of a field into displayable texts.
 *
 * Two shapes arrive here. A plain string is the server's message (see lib/validation/server-errors.ts),
 * already translated by the backend, and is shown verbatim. An object is a Zod issue, whose `message`
 * is one of our own markers — [REQUIRED] becomes the backend's wording for a missing value, with the
 * field's label as its argument. Anything else is Zod's own English default ("Invalid input: expected
 * string, received undefined"): a schema bug, never something a user should read, so it turns into the
 * generic message and is logged.
 */
function useFieldErrors(): (
  meta: { errors?: unknown[] },
  label: string
) => string[] {
  const t = useTranslations();
  return (meta, label) =>
    (meta.errors ?? [])
      .map((e) => {
        if (e == null) return null;
        if (typeof e === "string") return e;
        if (typeof e !== "object" || !("message" in e)) return null;
        const message = String((e as { message?: unknown }).message ?? "");
        if (message === REQUIRED)
          return t("validation.error.fieldRequired", { arg0: label });
        console.warn(`Untranslated validation error on "${label}": ${message}`);
        return t("validation.error.generic");
      })
      .filter((m): m is string => !!m);
}

/**
 * Ids tying a label to its control, so the control has an accessible name.
 *
 * `htmlFor` alone would do for an input, but the select's trigger is a `<button>`, which a label
 * cannot label — hence `labelId` for its `aria-labelledby`.
 */
interface FieldIds {
  controlId: string;
  labelId: string;
}

function useFieldIds(): FieldIds {
  const id = useId();
  return { controlId: `${id}-control`, labelId: `${id}-label` };
}

function FieldShell({
  label,
  required,
  hint,
  invalid,
  errors,
  className,
  ids,
  children,
}: {
  label: string;
  required?: boolean;
  hint?: string;
  invalid: boolean;
  errors: string[];
  className?: string;
  ids: FieldIds;
  children: ReactNode;
}) {
  return (
    <Field
      data-invalid={invalid || undefined}
      className={cn("gap-1.5", className)}
    >
      <FieldLabel
        id={ids.labelId}
        htmlFor={ids.controlId}
        className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground"
      >
        {label}
        {required && <span className="ml-0.5 text-primary">*</span>}
      </FieldLabel>
      {children}
      {hint && !invalid && <FieldDescription>{hint}</FieldDescription>}
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </Field>
  );
}

export function InputField({
  name,
  label,
  required,
  hint,
  className,
  type = "text",
  placeholder,
}: InputFieldProps) {
  const form = useBookEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as {
          isTouched: boolean;
          isValid: boolean;
          errors?: unknown[];
        };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = field.state.value as string | null;
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <Input
              id={ids.controlId}
              type={type}
              placeholder={placeholder}
              value={raw ?? ""}
              // An emptied optional field becomes null, which is how the backend stores "no value".
              // A required one keeps "" — its schema expects a string and would otherwise complain
              // about the type instead of the missing value (see requiredString).
              onChange={(e) =>
                field.handleChange(
                  required ? e.target.value : e.target.value || null
                )
              }
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

interface TextAreaFieldProps extends BaseProps {
  rows?: number;
}

export function TextAreaField({
  name,
  label,
  required,
  hint,
  className,
  rows = 4,
}: TextAreaFieldProps) {
  const form = useBookEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as {
          isTouched: boolean;
          isValid: boolean;
          errors?: unknown[];
        };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = field.state.value as string | null;
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <Textarea
              id={ids.controlId}
              rows={rows}
              value={raw ?? ""}
              // Same null-vs-"" rule as InputField.
              onChange={(e) =>
                field.handleChange(
                  required ? e.target.value : e.target.value || null
                )
              }
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectFieldProps extends BaseProps {
  options: SelectOption[];
  /**
   * Offers a button that sets the field back to null — for a column that allows it (e.g. BookDO's
   * `type`, `nullable = true`), matching the ✕ of the legacy page. Radix has no such affordance of
   * its own: an empty SelectItem value is forbidden, so "no value" is unreachable once one is set.
   */
  clearable?: boolean;
  /**
   * Renders the value larger and in the accent colour. For the status of a book, which is what a
   * reader looks for first and which the legacy page buried among the other fields.
   */
  emphasized?: boolean;
}

export function SelectField({
  name,
  label,
  required,
  hint,
  className,
  options,
  clearable,
  emphasized,
}: SelectFieldProps) {
  const form = useBookEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const tCommon = useTranslations();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as {
          isTouched: boolean;
          isValid: boolean;
          errors?: unknown[];
        };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = (field.state.value as string | null) ?? "";
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex items-center gap-1">
              <Select
                value={raw}
                // "" is never a choice a user can make — Radix forbids an empty SelectItem value —
                // so it can only come from its own hidden native <select> (SelectBubbleInput): that
                // mirrors every value change into the native element and dispatches a change event,
                // which comes back through here. Its <option>s exist only while SelectContent is
                // mounted, i.e. while the dropdown is open, so setting the value of a *closed*
                // select matches nothing, leaves it at "" and would wipe the field. This happens
                // whenever the value changes without the dropdown being opened — for us when the
                // loaded book resets the form (see BookEditForm) to something other than the default.
                onValueChange={(v) => {
                  if (v === "") return;
                  field.handleChange(v);
                }}
              >
                {/* The trigger is a button, which a <label htmlFor> cannot name — hence labelledby. */}
                <SelectTrigger
                  id={ids.controlId}
                  aria-labelledby={ids.labelId}
                  className={cn(
                    "flex-1",
                    emphasized &&
                      "h-9 border-primary/40 bg-primary/5 text-sm font-semibold text-primary data-[size=default]:h-9"
                  )}
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {options.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {clearable && raw !== "" && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-7 shrink-0 text-muted-foreground"
                  aria-label={`${tCommon("reset")}: ${label}`}
                  onClick={() => field.handleChange(null)}
                >
                  <HugeiconsIcon icon={Cancel01Icon} size={13} />
                </Button>
              )}
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
