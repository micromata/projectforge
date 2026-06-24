"use client";

import { useDynamicLayout } from "./dynamic-context";
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
          variant={mapStyleToVariant(action.style)}
          disabled={isFetching}
          onClick={() => callAction(action)}
        >
          {translate(action.title ?? action.id)}
        </Button>
      ))}
    </div>
  );
}

function mapStyleToVariant(
  style?: string
): "default" | "destructive" | "outline" | "secondary" | "ghost" {
  switch (style) {
    case "danger":
      return "destructive";
    case "primary":
    case "success":
      return "default";
    case "secondary":
      return "secondary";
    case "link":
      return "ghost";
    default:
      return "outline";
  }
}
