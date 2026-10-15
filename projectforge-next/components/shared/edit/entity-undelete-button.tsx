"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowTurnBackwardIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

interface Props {
  /** Runs the restore. Rejecting or reporting the server's refusal is the caller's job. */
  onUndelete: () => void | Promise<void>;
  disabled?: boolean;
}

/**
 * Brings a marked-as-deleted entry back — the only action its form offers (see entityAccess).
 *
 * Without a confirmation, unlike [EntityDeleteButton]: `UIButton.createUndeleteButton` asks nothing
 * either, and there is nothing to lose here — the entry the user is looking at is the deleted one, and
 * restoring it is the way back out of the delete rather than a step further into it. Hence `outline`
 * too, where the delete is `destructive`.
 *
 * The label is the backend's own (`undelete`), the same wording Wicket's button carries.
 */
export function EntityUndeleteButton({ onUndelete, disabled }: Props) {
  const t = useTranslations();

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={() => void onUndelete()}
      disabled={disabled}
      className="gap-1.5"
    >
      <HugeiconsIcon icon={ArrowTurnBackwardIcon} size={13} />
      {t("undelete")}
    </Button>
  );
}
