"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete01Icon,
  FloppyDiskIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

export interface BookEditActionsProps {
  onCancel: () => void;
  onDelete: () => void;
  isSaving: boolean;
  lastSavedLabel: string | null;
  isDirty: boolean;
}

export function BookEditActions({
  onCancel,
  onDelete,
  isSaving,
  lastSavedLabel,
  isDirty,
}: BookEditActionsProps) {
  const t = useTranslations("books.edit.actions");
  return (
    <div className="flex shrink-0 items-center gap-3 border-t border-border bg-background px-6 py-2.5 shadow-[0_-2px_12px_rgba(0,0,0,0.05)]">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onCancel}
        disabled={isSaving}
      >
        {t("cancel")}
      </Button>
      <Button
        type="submit"
        size="sm"
        disabled={isSaving || !isDirty}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={FloppyDiskIcon} size={14} />
        {t("save")}
      </Button>
      {lastSavedLabel && (
        <span className="text-xs text-muted-foreground">
          {t("lastSaved", { time: lastSavedLabel })}
        </span>
      )}
      <div className="flex-1" />
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onDelete}
        disabled={isSaving}
        className="gap-1.5"
        style={{
          borderColor: "var(--status-loaned-border)",
          color: "var(--status-loaned)",
        }}
      >
        <HugeiconsIcon icon={Delete01Icon} size={13} />
        {t("delete")}
      </Button>
    </div>
  );
}
