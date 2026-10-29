"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import type { Kost2ReallocationRun } from "@/lib/rs/kost2-reallocation";
import type { useRunActions } from "./use-run-actions";

type Confirm = "apply" | "revert" | "delete";

/**
 * The pinned action bar of the wizard: cancel, save, apply (a draft), re-apply (an applied run whose shares a DATEV
 * reimport discarded) and revert (an applied run); delete (a draft) on the far right. Applying, reverting and
 * deleting ask first.
 */
export function WizardActions({
  run,
  actions,
}: {
  run: Kost2ReallocationRun | null;
  actions: ReturnType<typeof useRunActions>;
}) {
  const t = useTranslations();
  const router = useRouter();
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const draft = !run || run.status === "DRAFT";
  const applied = run?.status === "APPLIED";
  // Literal keys: the i18n generator exports no template literals.
  const dialogs = {
    apply: {
      title: t("fibu.kost2Reallocation.apply"),
      text: t("fibu.kost2Reallocation.confirm.apply"),
      run: () => actions.apply.mutate(),
    },
    revert: {
      title: t("fibu.kost2Reallocation.revert"),
      text: t("fibu.kost2Reallocation.confirm.revert"),
      run: () => actions.revert.mutate(),
    },
    delete: {
      title: t("delete"),
      text: t("fibu.kost2Reallocation.confirm.delete"),
      run: () => actions.remove.mutate(),
    },
  };
  const dialog = dialogs[confirm ?? "apply"];

  return (
    <>
      <FormActionBar>
        <Button
          variant="outline"
          onClick={() => router.push("/kost2-reallocation")}
        >
          {t("cancel")}
        </Button>
        {draft && (
          <>
            <HintTooltip
              text={t("fibu.kost2Reallocation.tooltip.save")}
              side="top"
            >
              <Button
                variant="outline"
                disabled={actions.pending}
                onClick={() => actions.save.mutate()}
              >
                {t("save")}
              </Button>
            </HintTooltip>
            <HintTooltip
              text={t("fibu.kost2Reallocation.tooltip.apply")}
              side="top"
            >
              <Button
                disabled={actions.pending}
                onClick={() => setConfirm("apply")}
              >
                {t("fibu.kost2Reallocation.apply")}
              </Button>
            </HintTooltip>
          </>
        )}
        {applied && run?.needsReapply && (
          <HintTooltip
            text={t("fibu.kost2Reallocation.tooltip.reapply")}
            side="top"
          >
            <Button
              disabled={actions.pending}
              onClick={() => actions.reapply.mutate()}
            >
              {t("fibu.kost2Reallocation.reapply")}
            </Button>
          </HintTooltip>
        )}
        {applied && (
          <HintTooltip
            text={t("fibu.kost2Reallocation.tooltip.revert")}
            side="top"
          >
            <Button
              variant="outline"
              disabled={actions.pending}
              onClick={() => setConfirm("revert")}
            >
              {t("fibu.kost2Reallocation.revert")}
            </Button>
          </HintTooltip>
        )}
        <div className="flex-1" />
        {draft && run && (
          <Button
            variant="destructive"
            disabled={actions.pending}
            onClick={() => setConfirm("delete")}
          >
            {t("delete")}
          </Button>
        )}
      </FormActionBar>
      <ConfirmDialog
        open={confirm != null}
        onOpenChange={(open) => !open && setConfirm(null)}
        title={dialog.title}
        description={dialog.text}
        confirmLabel={dialog.title}
        destructive={confirm === "delete"}
        onConfirm={() => confirm && dialog.run()}
      />
    </>
  );
}
