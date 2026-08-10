"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";

interface Props {
  /** Runs the delete. Rejecting or reporting the server's refusal is the caller's job. */
  onDelete: () => void | Promise<void>;
  disabled?: boolean;
}

/**
 * Marks the entry as deleted after confirming.
 *
 * Deliberately not part of [EntityEditActions]: the confirmation carries its own state, and the
 * button only exists once the entry has been saved at least once.
 *
 * The labels are the backend's own (`markAsDeleted`, `question.markAsDeletedQuestion`) — the same
 * wording Wicket asks with.
 */
export function EntityDeleteButton({ onDelete, disabled }: Props) {
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
        <HugeiconsIcon icon={Delete01Icon} size={13} />
        {t("markAsDeleted")}
      </Button>
      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={t("markAsDeleted")}
        description={t("question.markAsDeletedQuestion")}
        confirmLabel={t("markAsDeleted")}
        destructive
        onConfirm={() => void onDelete()}
      />
    </>
  );
}
