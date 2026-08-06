"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { buttonVariant } from "../button-variant";
import { Button } from "@/components/ui/button";
import type { ActionDef } from "@/lib/rs/types";

/** A BUTTON element inside the layout. `UILayout.actions` are rendered by DynamicActionGroup. */
export function DynamicButton({ node }: DynamicComponentProps) {
  const { callAction, translate, isFetching } = useDynamicLayout();

  // A layout node of type BUTTON carries exactly the fields of UIButton.
  const action = node as unknown as ActionDef;
  const title = action.title ?? action.id;

  return (
    <Button
      variant={buttonVariant(action.color, action.outline)}
      disabled={isFetching || action.disabled}
      title={action.tooltip}
      onClick={() => callAction(action)}
    >
      {translate(title)}
    </Button>
  );
}
