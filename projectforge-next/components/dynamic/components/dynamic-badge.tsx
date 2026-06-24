"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Badge } from "@/components/ui/badge";

export function DynamicBadge({ node }: DynamicComponentProps) {
  const { translate } = useDynamicLayout();

  if (node.type === "BADGE_LIST") {
    const badges = (node.badges ?? []) as Array<{ title?: string; style?: string }>;
    return (
      <div className="flex flex-wrap gap-1">
        {badges.map((b, idx) => (
          <Badge key={idx} variant="secondary">
            {b.title ? translate(b.title) : ""}
          </Badge>
        ))}
      </div>
    );
  }

  const title = (node.title as string) ?? "";
  return <Badge variant="secondary">{translate(title)}</Badge>;
}
