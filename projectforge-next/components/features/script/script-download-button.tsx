"use client";

import { useState, type ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { RsError } from "@/lib/rs/client";
import { toast } from "@/lib/toast";

/**
 * A button fetching a file of the backend (see downloadFile): disabled while it loads, a refusal — an
 * expired result file — shown as a toast.
 */
export function ScriptDownloadButton({
  download,
  hint,
  children,
}: {
  download: () => Promise<void>;
  hint?: string;
  children: ReactNode;
}) {
  const [busy, setBusy] = useState(false);
  const onClick = async () => {
    setBusy(true);
    try {
      await download();
    } catch (error) {
      toast.error(error instanceof RsError ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  };
  return (
    <HintTooltip text={hint}>
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={busy}
        onClick={() => void onClick()}
      >
        <HugeiconsIcon icon={Download01Icon} size={14} aria-hidden />
        {children}
      </Button>
    </HintTooltip>
  );
}
