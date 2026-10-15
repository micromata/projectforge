"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { GridTableIcon } from "@hugeicons/core-free-icons";
import { Toggle } from "@/components/ui/toggle";
import { TaskWizardLink } from "@/components/shared/tasks/task-wizard-link";
import { useAccessMatrixViewStore } from "./access-matrix-view-store";

/**
 * The access list's toolbar actions: the task ("structure") wizard — the same "Assistent" the Wicket
 * `AccessListPage` offered its admins ([TaskWizardLink] renders nothing for a non-admin, so no gating
 * is needed) — and the pill that expands every row's permission matrix from the compact icon line to
 * the full labelled matrix ([AccessMatrixCell]) at once.
 *
 * The pill's state ([useAccessMatrixViewStore]) is remembered for the session's lifetime of the page:
 * the module-level store survives client navigation (opening an entry and coming back keeps the view),
 * and only a full page reload falls back to the compact default — page-local, not persisted. The
 * handed-in filter is not read — neither the wizard nor the view toggle acts on the current rows.
 */
export function AccessListActions() {
  const t = useTranslations();
  const view = useAccessMatrixViewStore((s) => s.view);
  const setView = useAccessMatrixViewStore((s) => s.setView);

  return (
    <>
      <Toggle
        variant="outline"
        size="sm"
        className="rounded-full"
        pressed={view === "detail"}
        onPressedChange={(pressed) => setView(pressed ? "detail" : "compact")}
      >
        <HugeiconsIcon icon={GridTableIcon} />
        {t("expand")}
      </Toggle>
      <TaskWizardLink />
    </>
  );
}
