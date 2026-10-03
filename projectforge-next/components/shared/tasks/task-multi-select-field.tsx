"use client";

import { useState } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityEditForm,
  useFieldMetadata,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import type { TaskNode } from "@/lib/rs/task";
import { TaskMultiSelectRow } from "./task-multi-select-row";
import { TaskSelectControl } from "./task-select-control";
import { TaskSelectModal } from "./task-select-modal";

/**
 * Picks any number of tasks for a hand-built form field that stores `EntityRef[]` — the tasks of a
 * business unit.
 *
 * The task select's counterpart of [EntityMultiAutocompleteField]: a plain type-ahead shows a bare
 * title, which only means something in its place in the tree (see [TaskSelect]). So every picked task
 * is listed with its path, and new ones are added through the same control as a single task — search,
 * favorites and the tree.
 */
export function TaskMultiSelectField({
  name,
  label,
  hint,
  className,
  disabled,
}: BaseFieldProps & {
  /** The tasks may be read but not changed. */
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
        const entries = (field.state.value as EntityRef[] | null) ?? [];

        const commit = (next: EntityRef[]) => {
          // Sorted by label, as the type-ahead this replaces kept them; one entry per task.
          const unique = next.filter(
            (entry, i) => next.findIndex((e) => e.id === entry.id) === i
          );
          field.handleChange(
            unique.sort((a, b) => a.displayName.localeCompare(b.displayName))
          );
          // Blurring by hand: the pickers are popovers and a dialog, so nothing else marks the field
          // touched (as in [TaskSelectField]).
          field.handleBlur();
        };
        /** The field stores the reference the form layer expects, as [TaskSelectField] does. */
        const toRef = (task: TaskNode): EntityRef => ({
          id: task.id,
          displayName: task.title ?? "",
        });
        const add = (task: TaskNode | null) => {
          if (task != null) commit([...entries, toRef(task)]);
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
            <div className="flex min-w-0 flex-col gap-1">
              {entries.length > 0 && (
                <ul className="flex min-w-0 flex-col gap-1">
                  {entries.map((entry, index) => (
                    <TaskMultiSelectRow
                      key={entry.id}
                      entry={entry}
                      disabled={disabled}
                      onReplace={(task) =>
                        commit(entries.with(index, toRef(task)))
                      }
                      onRemove={() =>
                        commit(entries.filter((_, i) => i !== index))
                      }
                    />
                  ))}
                </ul>
              )}
              <TaskSelectControl
                taskId={null}
                ariaLabel={label}
                disabled={disabled}
                showEditLink={false}
                onOpen={() => setOpen(true)}
                onSelect={add}
              />
            </div>
            <TaskSelectModal
              value={null}
              onChange={add}
              open={open}
              onOpenChange={setOpen}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
