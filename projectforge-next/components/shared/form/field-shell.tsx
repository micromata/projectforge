"use client";

import { useId, type ReactNode } from "react";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { cn } from "@/lib/utils";
import { FieldHint } from "./field-hint";

/**
 * What every field of a hand-built form takes.
 *
 * `required` and `maxLength` are deliberately not props: they are the backend's rules and come from
 * the generated metadata via [useFieldMetadata], the same source the Zod schema reads. A caller that
 * could set them would be a second place to maintain them — and the one that had drifted before.
 */
export interface BaseFieldProps {
  /** Name of the form value, which is also the key of its entry in the entity's metadata. */
  name: string;
  label: string;
  /** Explains the field; shown behind an ⓘ next to the label, see [FieldHint]. */
  hint?: string;
  className?: string;
}

/**
 * Ids tying a label to its control, so the control has an accessible name.
 *
 * `htmlFor` alone would do for an input, but the select's trigger is a `<button>`, which a label
 * cannot label — hence `labelId` for its `aria-labelledby`.
 */
export interface FieldIds {
  controlId: string;
  labelId: string;
}

export function useFieldIds(): FieldIds {
  const id = useId();
  return { controlId: `${id}-control`, labelId: `${id}-label` };
}

/** The slice of a @tanstack/react-form field's meta state the shell needs. */
export interface FieldMetaState {
  isTouched: boolean;
  isValid: boolean;
  errors?: unknown[];
}

export function FieldShell({
  name,
  label,
  required,
  readOnly,
  hint,
  additionalLabel,
  invalid,
  errors,
  warning,
  className,
  ids,
  children,
}: {
  /**
   * Name of the form value this field binds to, written out as `data-field` — how the field is found in
   * the DOM without knowing which component rendered it. What a form's `autoFocus` names
   * ([useFocusFirstField] looks the field up by it), and equally what an e2e test can address.
   *
   * Optional, because a shell may also wrap something that is not one form value (a cost assignment's
   * share of a position, a task picker built of several).
   */
  name?: string;
  label: string;
  required?: boolean;
  /** Shown but not fillable — suppresses the asterisk, see below. */
  readOnly?: boolean;
  hint?: string;
  /**
   * What the value *is*, under the box — "Posix account" below a group's GID number
   * (`UIInput.additionalLabel` of the server laid out form, see LdapGidField).
   *
   * Not a second label and not a [hint]: it names the context the value only makes sense in, which a
   * reader has to see without hovering anything, while the explanation of the field stays behind the ⓘ.
   */
  additionalLabel?: string;
  invalid: boolean;
  errors: string[];
  /** Something off about an otherwise valid value — see the field prop passing it in. */
  warning?: string;
  className?: string;
  ids: FieldIds;
  children: ReactNode;
}) {
  return (
    <Field
      data-field={name}
      data-invalid={invalid || undefined}
      className={cn("gap-1.5", className)}
    >
      {/* The ⓘ beside the label rather than inside it: a <label> forwards a click anywhere in it to
          its control, which would open the field instead of the explanation. */}
      {/* `items-start`, so the ⓘ stays on the first line of a label that wrapped rather than centring
          itself against two lines of it. */}
      <div className="flex min-w-0 items-start gap-1">
        <FieldLabel
          id={ids.labelId}
          htmlFor={ids.controlId}
          className="min-w-0 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground"
        >
          {label}
          {/* Not on a field the user cannot fill in: a value only the backend supplies (an order's
              number) is mandatory in the database but never the reader's obligation. */}
          {required && !readOnly && (
            <span className="ml-0.5 text-primary">*</span>
          )}
        </FieldLabel>
        {hint && <FieldHint hint={hint} label={label} />}
      </div>
      {children}
      {/* Under the box, as the legacy form shows it, and above the error: it belongs to the value, not
          to what is wrong with it. */}
      {additionalLabel && (
        <p className="text-xs italic text-muted-foreground">
          {additionalLabel}
        </p>
      )}
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
      {/* Below the error, and only while there is none: a rule the value breaks is the harder statement,
          and two sentences under one box read as one confused one. Not a `FieldError` and not part of
          `data-invalid` — the value is valid, and saying otherwise would mark a field the form is
          perfectly willing to save. `data-tone` names the tone for a test, as [FormAlert] does. */}
      {!invalid && warning && (
        <p data-tone="warning" className="text-xs text-warning">
          {warning}
        </p>
      )}
    </Field>
  );
}
