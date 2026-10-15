"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";

interface Props {
  /** Runs the irrevocable delete. Rejecting or reporting the server's refusal is the caller's job. */
  onForceDelete: () => void | Promise<void>;
  disabled?: boolean;
}

/**
 * Destroys the entry for good — the row and its whole change history, with no undo — after a stern
 * confirmation. The dangerous sibling of [EntityDeleteButton], offered only where the entity allows it
 * (`EditDef.forceDelete`, `isForceDeletionSupport`); the ordinary delete beside it merely marks.
 *
 * The labels are the backend's own (`forceDelete`, `question.forceDeleteQuestion`) — the same wording
 * Wicket asks with, and the same warning it spells out.
 */
export function EntityForceDeleteButton({ onForceDelete, disabled }: Props) {
  const t = useTranslations();
  const [confirming, setConfirming] = useState(false);

  return (
    <>
      <Button
        type="button"
        variant="destructive"
        size="sm"
        onClick={() => setConfirming(true)}
        disabled={disabled}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={Delete02Icon} size={13} />
        {t("forceDelete")}
      </Button>
      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={t("forceDelete")}
        description={t("question.forceDeleteQuestion")}
        confirmLabel={t("forceDelete")}
        destructive
        onConfirm={() => void onForceDelete()}
      />
    </>
  );
}
