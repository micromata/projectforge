"use client";

import { useStore } from "@tanstack/react-form";
import {
  useEntityEditForm,
  useFieldMetadata,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import type { FieldErrorMeta } from "@/components/shared/form/use-field-errors";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { TaskKost2Picker } from "../../task-kost2-picker";
import type { TimesheetEditValues } from "../timesheet-edit-schema";

/**
 * The task + cost unit block of the time sheet edit form — the form adapter around the shared
 * [TaskKost2Picker]. It reads and writes the two form values and surfaces their validation, while the
 * picker owns the dependency between them (see there); the mass update drives the same picker from local
 * state (see TaskKost2MassUpdateField).
 */
export function TaskKost2Section({ className }: { className?: string }) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const { required } = useFieldMetadata("kost2");

  const taskId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TimesheetEditValues).task?.id ?? null
  ) as number | null;
  const kost2Id = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TimesheetEditValues).kost2?.id ?? null
  ) as number | null;

  const taskErrors = useStore(form.store, (state: unknown) =>
    errorsOf(state, "task", fieldErrors, "task._")
  );
  const kost2Errors = useStore(form.store, (state: unknown) =>
    errorsOf(state, "kost2", fieldErrors, "fibu.kost2._")
  );

  return (
    <TaskKost2Picker
      className={className}
      taskId={taskId}
      kost2Id={kost2Id}
      onTaskChange={(task: EntityRef | null) =>
        form.setFieldValue("task", task)
      }
      onKost2Change={(id) =>
        form.setFieldValue("kost2", id != null ? { id } : null)
      }
      required={required}
      taskErrors={taskErrors}
      kost2Errors={kost2Errors}
      showConsumption
    />
  );
}

/**
 * The displayable errors of one field, only once it has been interacted with — the same gate the shared
 * fields apply (`isTouched && !isValid`), so an untouched required cost unit does not show its error on
 * first render.
 */
function errorsOf(
  state: unknown,
  name: string,
  fieldErrors: (meta: FieldErrorMeta, label: string) => string[],
  label: string
): string[] {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const meta = (state as any).fieldMeta?.[name] as
    | { isTouched?: boolean; isValid?: boolean; errors?: unknown[] }
    | undefined;
  if (!meta || !meta.isTouched || meta.isValid !== false) return [];
  return fieldErrors({ errors: meta.errors }, label);
}
