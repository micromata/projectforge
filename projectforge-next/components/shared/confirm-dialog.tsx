"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

export interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  /** Label of the confirming button, e.g. the translated "markAsDeleted". */
  confirmLabel: string;
  /** Label of the dismissing button; the translated "cancel" ("Abbrechen") unless a caller overrides it. */
  cancelLabel?: string;
  /** Destructive actions (delete) get the red button; anything else the default one. */
  destructive?: boolean;
  /** E.g. while what the action affects is still loading. */
  confirmDisabled?: boolean;
  /** Classes of the dialog, e.g. a larger width for a list in the description. */
  className?: string;
  onConfirm: () => void;
}

/**
 * Yes/no question before an irreversible action. The texts stay with the caller: they come from the
 * backend bundle (e.g. `question.markAsDeletedQuestion`) and differ per entity.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  cancelLabel,
  destructive,
  confirmDisabled,
  className,
  onConfirm,
}: ConfirmDialogProps) {
  const t = useTranslations();
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className={className}>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          {/* asChild so the description renders as a `div`: a caller may pass block content (e.g. the
              mass update's list of changes), which is invalid nested in the `p` the primitive renders. */}
          <AlertDialogDescription asChild>
            <div>{description}</div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{cancelLabel ?? t("cancel")}</AlertDialogCancel>
          <AlertDialogAction
            variant={destructive ? "destructive" : "default"}
            disabled={confirmDisabled}
            onClick={onConfirm}
          >
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
