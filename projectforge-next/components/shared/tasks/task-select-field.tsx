"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Edit02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
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
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import { TaskEditLink } from "./task-edit-link";
import { TaskPath } from "./task-path";
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
            <TaskSelectFieldContent
              taskId={taskId}
              ariaLabel={label}
              disabled={disabled}
              onOpen={() => setOpen(true)}
              onSelect={(task) => {
                field.handleChange(
                  task != null ? { id: task.id, displayName: task.title } : null
                );
                field.handleBlur();
              }}
            />
            <TaskSelectModal
              value={taskId}
              onChange={(task) => {
                field.handleChange(
                  task != null ? { id: task.id, displayName: task.title } : null
                );
                field.handleBlur();
              }}
              open={open}
              onOpenChange={setOpen}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

function TaskSelectFieldContent({
  taskId,
  ariaLabel,
  disabled,
  onOpen,
  onSelect,
}: {
  taskId: number | null;
  ariaLabel: string;
  disabled?: boolean;
  onOpen: () => void;
  onSelect: (task: TaskNode | null) => void;
}) {
  const t = useTranslations();

  const { data: task } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
  });

  return (
    <div className="flex min-w-0 items-center gap-2">
      <div className="min-w-0 flex-1">
        <TaskPath
          task={(taskId != null && task) || null}
          onSelect={(node) => onSelect(node)}
          disabled={disabled}
        />
      </div>
      <Button
        type="button"
        variant="outline"
        size="icon"
        disabled={disabled}
        aria-label={t("task.tree.title.select") + " " + ariaLabel}
        onClick={onOpen}
        className="size-7 shrink-0"
      >
        <HugeiconsIcon icon={Edit02Icon} size={14} />
      </Button>
      {/* Leads to the task itself, where its timesheets are — see TaskEditLink. */}
      <TaskEditLink taskId={taskId} />
    </div>
  );
}
