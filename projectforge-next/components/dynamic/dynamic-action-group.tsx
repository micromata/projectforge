"use client";

import { useState } from "react";
import { useDynamicLayout } from "./dynamic-context";
import { buttonVariant } from "./button-variant";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";

export function DynamicActionGroup() {
  const { ui, callAction, translate, isFetching } = useDynamicLayout();
  // Which button is waiting for its answer. `isFetching` is the whole page's state and would put a
  // spinner on every button at once; a save can take seconds (a mail sent along with it waits for the
  // SMTP server), and what the user needs to see is that the button they pressed is working.
  const [pendingId, setPendingId] = useState<string | null>(null);
  const actions = ui.actions;

  if (!actions || actions.length === 0) return null;

  async function run(id: string, call: () => Promise<void>): Promise<void> {
    setPendingId(id);
    try {
      await call();
    } finally {
      // Also when the action navigated away — this component may still be mounted.
      setPendingId(null);
    }
  }

  return (
    <div className="flex items-center gap-2 border-t bg-background px-6 py-3">
      {actions.map((action) => (
        <HintTooltip key={action.id} text={action.tooltip}>
          <Button
            variant={buttonVariant(action.color, action.outline)}
            disabled={isFetching || action.disabled}
            aria-busy={pendingId === action.id}
            className="gap-1.5"
            onClick={() => void run(action.id, () => callAction(action))}
          >
            {pendingId === action.id && (
              <Spinner className="h-3.5 w-3.5 border-2" />
            )}
            {translate(action.title ?? action.id)}
          </Button>
        </HintTooltip>
      ))}
    </div>
  );
}
