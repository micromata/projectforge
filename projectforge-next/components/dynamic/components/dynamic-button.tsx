"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Button } from "@/components/ui/button";
import type { ActionDef } from "@/lib/rs/types";

export function DynamicButton({ node }: DynamicComponentProps) {
  const { callAction, translate, isFetching } = useDynamicLayout();

  const title = (node.title as string) ?? "";
  const style = (node.style as string) ?? "default";
  const id = node.id as string;

  const action: ActionDef = {
    id,
    title,
    style,
    url: node.url as string | undefined,
    responseAction: node.responseAction as ActionDef["responseAction"],
    confirmMessage: node.confirmMessage as string | undefined,
  };

  const variant = mapStyleToVariant(style);

  return (
    <Button
      variant={variant}
      disabled={isFetching}
      onClick={() => callAction(action)}
    >
      {translate(title)}
    </Button>
  );
}

function mapStyleToVariant(
  style: string
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
