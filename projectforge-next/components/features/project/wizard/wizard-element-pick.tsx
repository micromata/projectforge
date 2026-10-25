"use client";

import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchTaskInfo } from "@/lib/rs/task";
import type { ValidationError } from "@/lib/rs/types";
import type { ElementMode } from "./types";
import { WizardFieldError } from "./wizard-field-error";
import { WizardModeRadio } from "./wizard-mode-radio";
import { WizardTaskPicker } from "./wizard-task-picker";

interface WizardElementPickProps {
  /** Prefix of the radio ids. */
  id: string;
  label: string;
  intro: string;
  mode: ElementMode;
  onModeChange: (mode: ElementMode) => void;
  /** The picked existing element. */
  taskId: number | null;
  onTaskChange: (taskId: number | null) => void;
  /**
   * The existing element the server uses in place of a new one (`ProjectWizardRest.ValidateResponse`):
   * with mode "new" the one it found by the title, which is said here.
   */
  reusedTaskId: number | null;
  /** With mode "existing": why the picked element may not be a good choice, which doesn't keep from finishing. */
  warning?: string | null;
  /** The field id of the element's errors (`customerTaskId`, `projectTaskId`). */
  fieldId: string;
  errors: ValidationError[];
  /** What mode "new" asks for besides, e.g. the parent of a new customer element. */
  children?: ReactNode;
}

/**
 * A structure element of the customer or the project: a new one, or an existing one picked from the tree,
 * whose title may differ from the name (e.g. "ACME" for "ACME Inc."). A new one whose title exists already
 * below its parent is not created; the server uses the existing one, which is said here.
 */
export function WizardElementPick({
  id,
  label,
  intro,
  mode,
  onModeChange,
  taskId,
  onTaskChange,
  reusedTaskId,
  warning,
  fieldId,
  errors,
  children,
}: WizardElementPickProps) {
  const t = useTranslations();
  const found = mode === "new" ? reusedTaskId : null;
  // The same query as the select control's, so the title costs no second request.
  const { data: foundTask } = useQuery({
    queryKey: ["taskInfo", found],
    queryFn: ({ signal }) => fetchTaskInfo(found!, signal),
    enabled: found != null,
    staleTime: Infinity,
  });
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-sm font-medium">{label}</h3>
      <p className="text-xs text-muted-foreground">{intro}</p>
      <WizardModeRadio<ElementMode>
        id={id}
        value={mode}
        onChange={onModeChange}
        ariaLabel={label}
        options={[
          { value: "new", label: t("project.wizard.element.new") },
          { value: "existing", label: t("project.wizard.element.existing") },
        ]}
      />
      {mode === "existing" ? (
        <>
          <WizardTaskPicker
            taskId={taskId}
            onChange={onTaskChange}
            ariaLabel={label}
            errors={errors}
            fieldId={fieldId}
          />
          {warning && <p className="text-xs text-warning">{warning}</p>}
        </>
      ) : (
        <>
          {children}
          {foundTask && (
            <p className="text-xs text-muted-foreground">
              {t("project.wizard.element.autoReuse", {
                arg0: foundTask.title ?? "",
              })}
            </p>
          )}
          <WizardFieldError errors={errors} fieldId={fieldId} />
        </>
      )}
    </div>
  );
}
