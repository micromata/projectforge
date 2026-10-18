"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { GitCompareIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { CodeDiffDialog } from "./code-diff-dialog";
import type { CodeDiffViewProps } from "./code-diff-view";

/**
 * The editor toolbar's "show changes": opens the diff of the editor's code against the stored one
 * ([CodeDiffDialog]). Disabled as long as nothing was changed — there would be nothing to see.
 */
export function CodeDiffButton({
  original,
  current,
  language,
}: Omit<CodeDiffViewProps, "className">) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const label = t("codeEditor.diff._");
  return (
    <>
      <HintTooltip title={label}>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={label}
          disabled={original === current}
          onClick={() => setOpen(true)}
        >
          <HugeiconsIcon icon={GitCompareIcon} strokeWidth={2} />
        </Button>
      </HintTooltip>
      <CodeDiffDialog
        open={open}
        onOpenChange={setOpen}
        original={original}
        current={current}
        language={language}
      />
    </>
  );
}
