"use client";

import { useDynamicLayout } from "./dynamic-context";
import { buttonVariant } from "./button-variant";
import { Button } from "@/components/ui/button";

export function DynamicActionGroup() {
  const { ui, callAction, translate, isFetching } = useDynamicLayout();
  const actions = ui.actions;

  if (!actions || actions.length === 0) return null;

  return (
    <div className="flex items-center gap-2 border-t bg-background px-6 py-3">
      {actions.map((action) => (
        <Button
          key={action.id}
          variant={buttonVariant(action.color, action.outline)}
          disabled={isFetching || action.disabled}
          title={action.tooltip}
          onClick={() => callAction(action)}
        >
          {translate(action.title ?? action.id)}
        </Button>
      ))}
    </div>
  );
}
