"use client";

import { MarkdownText } from "@/components/shared/markdown-text";
import { RichText } from "@/components/shared/rich-text";

/**
 * The tooltip of the planning date: its generic explanation, then the hint finance and controlling
 * configured (parameter `fibu.forecast`), e.g. which planning date to choose for which year.
 */
export function PlanningDateTooltip({
  text,
  hint,
}: {
  text: string;
  hint: string;
}) {
  return (
    <div className="grid gap-2">
      <MarkdownText text={text} />
      <div className="border-t pt-2">
        <RichText html={hint} />
      </div>
    </div>
  );
}
