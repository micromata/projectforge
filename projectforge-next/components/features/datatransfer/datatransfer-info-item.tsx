import type { ReactNode } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";

/**
 * One value of the file view's info card: a label over it, the label's explanation as tooltip. Nothing
 * is rendered for an empty value — a row of dashes says less than the row not being there.
 */
export function DataTransferInfoItem({
  label,
  hint,
  children,
  className,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
  className?: string;
}) {
  if (children == null || children === "") return null;
  return (
    <div className={cn("flex min-w-0 flex-col gap-1", className)}>
      <dt className="text-xs font-medium text-muted-foreground">
        <HintTooltip text={hint} openOnTap>
          <span>{label}</span>
        </HintTooltip>
      </dt>
      <dd className="text-sm break-words whitespace-pre-line">{children}</dd>
    </div>
  );
}
