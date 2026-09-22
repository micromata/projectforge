"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Cancel01Icon,
  CheckmarkCircle02Icon,
  CheckmarkSquare02Icon,
  RefreshIcon,
  SquareIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

interface Props {
  /** Whether the stash has already been reconciled with the database (enables commit). */
  hasBeenReconciled: boolean;
  selectedCount: number;
  totalSelectable: number;
  onReconcile: () => void;
  onCommit: () => void;
  onCancel: () => void;
  onSelectAll: () => void;
  onDeselectAll: () => void;
  isReconciling?: boolean;
  isCommitting?: boolean;
  isCancelling?: boolean;
}

/**
 * The action bar of the preview step: reconcile the stash against the database, tick or clear all
 * importable rows, commit the ticked ones (enqueues the background job) or cancel the whole upload.
 * Commit is barred until a reconcile has run and something importable is ticked — the same gate the
 * backend enforces, surfaced here so the button does not offer a call that would only be refused.
 */
export function ImportControls({
  hasBeenReconciled,
  selectedCount,
  totalSelectable,
  onReconcile,
  onCommit,
  onCancel,
  onSelectAll,
  onDeselectAll,
  isReconciling,
  isCommitting,
  isCancelling,
}: Props) {
  const t = useTranslations();
  const allSelected = totalSelectable > 0 && selectedCount >= totalSelectable;

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Button
        type="button"
        variant="outline"
        onClick={onReconcile}
        disabled={isReconciling}
      >
        <HugeiconsIcon icon={RefreshIcon} />
        {t("common.import.action.reconcile")}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={allSelected ? onDeselectAll : onSelectAll}
        disabled={totalSelectable === 0}
      >
        <HugeiconsIcon
          icon={allSelected ? SquareIcon : CheckmarkSquare02Icon}
        />
        {allSelected
          ? t("common.import.action.deselectAll")
          : t("common.import.action.selectAll")}
      </Button>
      <Button
        type="button"
        onClick={onCommit}
        disabled={!hasBeenReconciled || selectedCount === 0 || isCommitting}
      >
        <HugeiconsIcon icon={CheckmarkCircle02Icon} />
        {t("common.import.action.commit")}
      </Button>
      <Button
        type="button"
        variant="destructive"
        onClick={onCancel}
        disabled={isCancelling}
        className="ml-auto"
      >
        <HugeiconsIcon icon={Cancel01Icon} />
        {t("common.import.clearStorage")}
      </Button>
    </div>
  );
}
