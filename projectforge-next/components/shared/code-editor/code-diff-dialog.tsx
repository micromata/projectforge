"use client";

import { lazy, Suspense } from "react";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useIsMobile } from "@/hooks/use-mobile";
import type { CodeDiffViewProps } from "./code-diff-view";

const CodeDiffView = lazy(() => import("./code-diff-view"));

/**
 * What changed in the editor since the code was stored: the editor's "show changes" button
 * (CodeEditorToolbar) opens it. A look only — the code is edited in the editor, not here.
 */
export function CodeDiffDialog({
  open,
  onOpenChange,
  original,
  current,
  language,
}: Omit<CodeDiffViewProps, "className"> & {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations();
  const unified = useIsMobile();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[85vh] w-[95vw] !max-w-6xl flex-col">
        <DialogHeader>
          <DialogTitle>{t("codeEditor.diff.title")}</DialogTitle>
        </DialogHeader>
        {original === current ? (
          <p className="flex-1 text-sm text-muted-foreground">
            {t("codeEditor.diff.none")}
          </p>
        ) : (
          <div className="flex min-h-0 flex-1 flex-col gap-1">
            {/* Which side is which; the unified diff tells its lines apart by colour alone. */}
            {!unified && (
              <div className="grid grid-cols-2 text-xs font-medium text-muted-foreground">
                <span>{t("codeEditor.diff.stored")}</span>
                <span>{t("codeEditor.diff.current")}</span>
              </div>
            )}
            <Suspense
              fallback={
                <div className="flex flex-1 items-center justify-center">
                  <Spinner className="h-5 w-5 border-2" />
                </div>
              }
            >
              <CodeDiffView
                original={original}
                current={current}
                language={language}
                className="min-h-0 flex-1"
              />
            </Suspense>
          </div>
        )}
        <DialogFooter>
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
