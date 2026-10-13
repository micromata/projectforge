"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlayIcon } from "@hugeicons/core-free-icons";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { useSubmitShortcutHint } from "@/hooks/use-submit-shortcut";

/** The action bar of the execution page: back, and execute (a spinner while the script runs). */
export function ScriptExecuteActions({
  onBack,
  onExecute,
  disabled,
  running,
}: {
  onBack: () => void;
  onExecute: () => void;
  disabled: boolean;
  running: boolean;
}) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  return (
    <FormActionBar className="mx-auto max-w-5xl">
      <Button type="button" variant="outline" onClick={onBack}>
        {t("back")}
      </Button>
      <HintTooltip {...shortcutHint}>
        <Button type="button" disabled={disabled} onClick={onExecute}>
          {running ? (
            <Spinner className="h-3.5 w-3.5 border-2" />
          ) : (
            <HugeiconsIcon icon={PlayIcon} size={14} aria-hidden />
          )}
          {t("execute")}
        </Button>
      </HintTooltip>
    </FormActionBar>
  );
}
