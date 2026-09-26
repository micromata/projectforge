"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { toast } from "@/lib/toast";
import type { SystemMessageResponse } from "@/lib/rs/system";

/** One administration action rendered as a button (see [SystemActionGroups]). */
export interface SystemAction {
  /** Stable key for React and for finding the action in tests. */
  key: string;
  /** i18n key of the button label. */
  labelKey: string;
  /** i18n key of the hover explanation, if any. */
  tooltipKey?: string;
  /** What the button does: a message action ({ message }) or a file download (void). */
  run?: () => Promise<SystemMessageResponse | void>;
  /** i18n key of a yes/no question asked before [run] fires. */
  confirmKey?: string;
  /** ICU values for [confirmKey] (the backend `{0}`/`{1}` placeholders become named args "0"/"1"). */
  confirmValues?: Record<string, string | number>;
  /** Rendered disabled (e.g. "Dump database", not yet migrated) with [disabledTooltipKey]. */
  disabled?: boolean;
  disabledTooltipKey?: string;
}

/**
 * A single administration action. A message action toasts its translated result; a download action
 * lets the browser save the file and only toasts on failure; a confirming action asks first.
 */
export function SystemActionButton({ action }: { action: SystemAction }) {
  const t = useTranslations();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const mutation = useMutation({
    mutationFn: () => action.run?.() ?? Promise.resolve(),
    onSuccess: (res) => {
      if (res && "message" in res && res.message) toast.success(res.message);
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  const onClick = () => {
    if (action.confirmKey) setConfirmOpen(true);
    else mutation.mutate();
  };

  const button = (
    <Button
      variant="outline"
      size="sm"
      disabled={action.disabled || mutation.isPending}
      onClick={onClick}
    >
      {t(action.labelKey)}
    </Button>
  );

  const tooltipKey =
    action.disabled && action.disabledTooltipKey
      ? action.disabledTooltipKey
      : action.tooltipKey;

  return (
    <>
      {tooltipKey ? (
        <HintTooltip text={t(tooltipKey)} openOnTap>
          {/* A disabled button emits no hover events, so the span carries the trigger. */}
          <span className="inline-flex">{button}</span>
        </HintTooltip>
      ) : (
        button
      )}
      {action.confirmKey && (
        <ConfirmDialog
          open={confirmOpen}
          onOpenChange={setConfirmOpen}
          title={t(action.labelKey)}
          description={t(action.confirmKey, action.confirmValues)}
          confirmLabel={t(action.labelKey)}
          onConfirm={() => mutation.mutate()}
        />
      )}
    </>
  );
}
