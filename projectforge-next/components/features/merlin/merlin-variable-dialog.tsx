"use client";

import { useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import {
  deleteMerlinVariable,
  updateMerlinVariable,
  type MerlinVariable,
} from "@/lib/rs/merlin";
import { MerlinVariableDialogFields } from "./merlin-variable-dialog-fields";
import { useMerlinVariables } from "./use-merlin-variables";

/**
 * Edits one variable of the template, or adds one (`variable` without id). "Apply" lets the backend fit the
 * change into the variables (`merlin/variable/update`: a variable with a master becomes a dependent one, a
 * renamed master is renamed in its dependent variables) and puts the result into the form — saved with the
 * form, not here. Only an unused variable can be removed: one the Word® template uses would come back with
 * the next analysis.
 */
export function MerlinVariableDialog({
  variable,
  onClose,
}: {
  variable: MerlinVariable;
  onClose: () => void;
}) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  const { variables, currentTemplate, apply } = useMerlinVariables();
  const [draft, setDraft] = useState<MerlinVariable>(variable);
  const set = (patch: Partial<MerlinVariable>) =>
    setDraft((prev) => ({ ...prev, ...patch }));
  const masterCandidates = variables.filter(
    (it) => it.id !== variable.id && (it.allowedValues?.length ?? 0) > 0
  );

  const write = useMutation({
    mutationFn: (remove: boolean) =>
      (remove ? deleteMerlinVariable : updateMerlinVariable)(
        currentTemplate(),
        draft
      ),
    onSuccess: (result) => {
      apply(result);
      onClose();
    },
  });
  const canApply = !write.isPending && draft.name.trim().length > 0;
  const submitShortcut = useSubmitShortcut(() => write.mutate(false), canApply);
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    // Return on one of the dialog's own buttons (cancel, delete) is that button's click.
    if (event.target instanceof HTMLButtonElement) return;
    submitShortcut(event);
  };

  return (
    // React bubbles the events of the portal along the component tree — into the edit form around, whose
    // own Return shortcut would save the template while this dialog is open.
    <div className="contents" onKeyDown={(event) => event.stopPropagation()}>
      <Dialog open onOpenChange={(open) => !open && onClose()}>
        <DialogContent className="sm:max-w-2xl" onKeyDown={onKeyDown}>
          <DialogHeader>
            <DialogTitle>
              {t(
                variable.id != null
                  ? "plugins.merlin.variable.edit"
                  : "plugins.merlin.variable.add"
              )}
            </DialogTitle>
          </DialogHeader>
          {write.isError && (
            <FormAlert tone="error">{t("errorpage.title")}</FormAlert>
          )}
          <MerlinVariableDialogFields
            draft={draft}
            set={set}
            masterCandidates={masterCandidates}
          />
          <DialogFooter className="sm:justify-between">
            <div>
              {variable.id != null && variable.used === false && (
                <Button
                  type="button"
                  variant="destructive"
                  disabled={write.isPending}
                  onClick={() => write.mutate(true)}
                >
                  {t("delete")}
                </Button>
              )}
            </div>
            <div className="flex flex-col-reverse gap-2 sm:flex-row">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("cancel")}
              </Button>
              <HintTooltip {...shortcutHint}>
                <Button
                  type="button"
                  disabled={!canApply}
                  onClick={() => write.mutate(false)}
                >
                  {write.isPending && <Spinner className="h-4 w-4 border-2" />}
                  {t("update")}
                </Button>
              </HintTooltip>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
