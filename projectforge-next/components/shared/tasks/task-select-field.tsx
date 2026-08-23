"use client";

import { useState } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityEditForm,
  useFieldMetadata,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import type { BaseFieldProps } from "@/components/shared/form/field-shell";
import { TaskSelectControl } from "./task-select-control";
import { TaskSelectModal } from "./task-select-modal";

/**
 * Picks a task for a hand-built form field that stores an [EntityRef].
 *
 * Bridges the gap between the form layer (which stores `{id, displayName}`) and [TaskSelectModal]
 * (which works with a plain task id). The breadcrumb path is always visible; the tree opens in a
 * dialog so the position row stays compact.
 */
export function TaskSelectField({
  name,
  label,
  hint,
  className,
  disabled,
}: BaseFieldProps & {
  /** The path may be read but not changed (see DeclaredField.readOnly). */
  disabled?: boolean;
}) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name);
  const [open, setOpen] = useState(false);

  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const ref = (field.state.value as EntityRef | null) ?? null;
        const taskId = ref?.id ?? null;

        /** The field stores the reference the form layer expects, not the whole node. */
        const change = (task: { id: number; title?: string } | null) => {
          field.handleChange(
            task != null ? { id: task.id, displayName: task.title } : null
          );
          field.handleBlur();
        };

        return (
          <FieldShell
            label={label}
            required={required}
            readOnly={disabled}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <TaskSelectControl
              taskId={taskId}
              ariaLabel={label}
              disabled={disabled}
              onOpen={() => setOpen(true)}
              onSelect={change}
            />
            <TaskSelectModal
              value={taskId}
              onChange={change}
              open={open}
              onOpenChange={setOpen}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
