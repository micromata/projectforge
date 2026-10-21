"use client";

import { useState } from "react";
import { CodeEditor } from "@/components/shared/code-editor/code-editor";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { prettyJson } from "@/lib/pretty-json";
import type { JsonEditorProps } from "./json-editors";

/**
 * The value of a JSON parameter as JSON text, in the code editor: indented on opening (the backend stores
 * it compact), written back as typed. The backend's errors (ConfigurationJsonValidators) arrive on
 * `stringValue` and are shown below it. An emptied editor is null, an unset value.
 */
export function ConfigurationJsonRawEditor({
  label,
  hint,
  className,
  readOnly,
}: JsonEditorProps & { readOnly: boolean }) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  // The text as typed. Shown as long as it is the form value: once the value is set otherwise (the form is
  // reset after a save, say), it is shown indented again. Merely opening the editor changes nothing.
  const [draft, setDraft] = useState<string | null>(null);
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const value = (field.state.value as string | null) ?? "";
        // A blank draft is written as null, so it is still the value then.
        const typed =
          draft !== null &&
          (draft === value || (value === "" && draft.trim() === ""));
        return (
          <FieldShell
            name="stringValue"
            label={label}
            readOnly={readOnly}
            hint={hint}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <CodeEditor
              id={ids.controlId}
              ariaLabel={label}
              value={typed ? draft : prettyJson(value)}
              onChange={(next) => {
                setDraft(next);
                field.handleChange(next.trim().length > 0 ? next : null);
              }}
              onBlur={field.handleBlur}
              language="json"
              minHeight="16rem"
              readOnly={readOnly}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
