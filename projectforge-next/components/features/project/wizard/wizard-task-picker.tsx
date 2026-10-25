"use client";

import { useState } from "react";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import type { ValidationError } from "@/lib/rs/types";
import { WizardFieldError } from "./wizard-field-error";

/**
 * One structure element of the wizard: the select control, its tree dialog and the server's complaint
 * about it. Built from the control and the dialog directly (like WizardTaskStep), as this is no entity form.
 */
export function WizardTaskPicker({
  taskId,
  onChange,
  ariaLabel,
  errors,
  fieldId,
}: {
  taskId: number | null;
  onChange: (taskId: number | null) => void;
  ariaLabel: string;
  errors: ValidationError[];
  /** The field id of its errors, see ProjectWizardRest. */
  fieldId: string;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="flex flex-col gap-1.5">
      <TaskSelectControl
        taskId={taskId}
        ariaLabel={ariaLabel}
        onOpen={() => setOpen(true)}
        onSelect={(task) => onChange(task?.id ?? null)}
      />
      <TaskSelectModal
        value={taskId}
        onChange={(task) => onChange(task?.id ?? null)}
        open={open}
        onOpenChange={setOpen}
      />
      <WizardFieldError errors={errors} fieldId={fieldId} />
    </div>
  );
}
