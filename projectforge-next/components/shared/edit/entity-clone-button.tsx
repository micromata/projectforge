"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

interface Props {
  /** Runs the clone and navigates to the new entry. Reporting a failure is the caller's job. */
  onClone: () => void | Promise<void>;
  disabled?: boolean;
}

/**
 * Builds a new entry from the one being edited — the recurring monthly invoice is what it exists for
 * (Wicket's `RechnungEditPage.cloneData`).
 *
 * No confirmation, unlike [EntityDeleteButton]: nothing is destroyed and nothing is written either.
 * The clone opens as an unsaved new entry, so the way back is to leave the page.
 *
 * `clone` is the backend's own label, so it reads the same here as in Wicket.
 */
export function EntityCloneButton({ onClone, disabled }: Props) {
  const t = useTranslations();

  return (
    <Button
      type="button"
      variant="secondary"
      size="sm"
      onClick={() => void onClone()}
      disabled={disabled}
      className="gap-1.5"
    >
      <HugeiconsIcon icon={Copy01Icon} size={14} />
      {t("clone")}
    </Button>
  );
}
