"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { GitCompareIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { CodeDiffDialog } from "./code-diff-dialog";
import type { CodeDiffViewProps } from "./code-diff-view";
import type { CodeVersionSource } from "./types";

/**
 * The editor toolbar's "show changes": opens the diff of the editor's code against the stored one
 * ([CodeDiffDialog]). Without former versions to compare as well, disabled as long as nothing was
 * changed — there would be nothing to see.
 */
export function CodeDiffButton({
  original,
  current,
  language,
  versions,
}: Omit<CodeDiffViewProps, "className"> & { versions?: CodeVersionSource }) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const label = t("codeEditor.diff._");
  const unchanged = original === current && !versions;
  const button = (
    <Button
      type="button"
      variant="ghost"
      size="icon-sm"
      aria-label={label}
      disabled={unchanged}
      onClick={() => setOpen(true)}
    >
      <HugeiconsIcon icon={GitCompareIcon} strokeWidth={2} />
    </Button>
  );
  return (
    <>
      <HintTooltip
        title={label}
        text={unchanged ? t("codeEditor.diff.none") : undefined}
      >
        {/* Wrapped while disabled: a disabled button receives no hover, and the tooltip is what tells
            why it is (see ExportButton). */}
        {unchanged ? <span tabIndex={0}>{button}</span> : button}
      </HintTooltip>
      <CodeDiffDialog
        open={open}
        onOpenChange={setOpen}
        original={original}
        current={current}
        language={language}
        versions={versions}
      />
    </>
  );
}
