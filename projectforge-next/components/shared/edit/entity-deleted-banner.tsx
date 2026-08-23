"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon } from "@hugeicons/core-free-icons";

/**
 * Says that the entry on screen is marked as deleted, and why it cannot be edited.
 *
 * Between the tab strip and the sections, i.e. above everything the user could reach for, and in the
 * destructive colours the list marks a deleted row with. Without it the state was legible from one
 * thing only — that the page offers a restore instead of a save — which is a conclusion, not a
 * statement (the fields being read-only looked like a missing right).
 */
export function EntityDeletedBanner() {
  const t = useTranslations();
  return (
    <div
      // Not an alert: nothing happened just now, this is the state of what is being looked at, so a
      // screen reader gets it in reading order like the heading and not as an interruption.
      className="flex items-center gap-2 border-b border-destructive/30 bg-destructive/10 px-6 py-1.5 text-xs font-medium text-destructive"
    >
      <HugeiconsIcon icon={AlertCircleIcon} size={14} aria-hidden />
      <span>{t("entityEdit.deletedInfo")}</span>
    </div>
  );
}
