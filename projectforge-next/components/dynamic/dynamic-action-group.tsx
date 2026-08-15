"use client";

import { useDynamicLayout } from "./dynamic-context";
import { buttonVariant } from "./button-variant";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

export function DynamicActionGroup() {
  const { ui, callAction, translate, isFetching } = useDynamicLayout();
  const actions = ui.actions;

  if (!actions || actions.length === 0) return null;

  return (
    <div className="flex items-center gap-2 border-t bg-background px-6 py-3">
      {actions.map((action) => (
        <HintTooltip key={action.id} text={action.tooltip}>
          <Button
            variant={buttonVariant(action.color, action.outline)}
            disabled={isFetching || action.disabled}
            onClick={() => callAction(action)}
          >
            {translate(action.title ?? action.id)}
          </Button>
        </HintTooltip>
      ))}
    </div>
  );
}
