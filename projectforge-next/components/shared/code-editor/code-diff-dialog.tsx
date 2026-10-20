"use client";

import { useId, useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import type { CodeDiffViewProps } from "./code-diff-view";
import { CodeDiffBody } from "./code-diff-body";
import type { CodeVersionSource } from "./types";

export type CodeDiffDialogProps = Omit<
  CodeDiffViewProps,
  "className" | "showAll" | "original"
> & {
  /** The stored code; undefined while it is still being fetched (a spinner is shown meanwhile). */
  original: string | undefined;
  /** The former versions to choose from besides the stored and the edited code. */
  versions?: CodeVersionSource;
  /** The name of the stored version in the pickers, where "stored" doesn't fit (a file, say). */
  storedLabel?: string;
};

/**
 * What changed in the editor since the code was stored, or between any two of its versions where it
 * has former ones ([CodeVersionSource]): the editor's "show changes" button (CodeEditorToolbar) opens
 * it. A look only — the code is edited in the editor, not here.
 */
export function CodeDiffDialog({
  open,
  onOpenChange,
  title,
  options,
  ...props
}: CodeDiffDialogProps & {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Instead of "compare versions". */
  title?: string;
  /** Further switches of the diff, beside "show all lines". */
  options?: ReactNode;
}) {
  const t = useTranslations();
  const showAllId = useId();
  // Kept across openings: whoever wants every line (to copy a former version whole) will again.
  const [showAll, setShowAll] = useState(false);
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[85vh] w-[95vw] !max-w-6xl flex-col">
        <DialogHeader>
          <DialogTitle>{title ?? t("codeEditor.diff.title")}</DialogTitle>
        </DialogHeader>
        {/* Mounted with each opening, so the choice of versions starts afresh (useCodeVersions). */}
        <CodeDiffBody {...props} showAll={showAll} />
        <DialogFooter>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 sm:mr-auto">
            {options}
            <div className="flex items-center gap-2">
              <Switch
                id={showAllId}
                checked={showAll}
                onCheckedChange={setShowAll}
              />
              <Label htmlFor={showAllId}>{t("codeEditor.diff.showAll")}</Label>
            </div>
          </div>
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("close")}
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
