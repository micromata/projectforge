import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface SectionHeaderProps {
  title: string;
  className?: string;
  /** Before the title — the chevron of a section that folds (see DeclaredSection). */
  leading?: ReactNode;
  /**
   * After the rule, at the right end of the line: an action about the whole section rather than about one
   * of its fields (see SectionDef.headerActions).
   */
  trailing?: ReactNode;
}

/**
 * The heading of a section card. Its title takes the accent colour while the section is the active
 * one — the outline around the card is [SectionCard]'s part of that, and the rule inside it stays
 * neutral so the two don't compete.
 */
export function SectionHeader({
  title,
  className,
  leading,
  trailing,
}: SectionHeaderProps) {
  return (
    <div className={cn("mb-4 flex items-center gap-3.5", className)}>
      {leading}
      <span className="whitespace-nowrap text-xs font-bold uppercase tracking-wide text-foreground/85 transition-colors group-data-[active=true]/section:text-primary">
        {title}
      </span>
      {/* The rule takes what is left, so the action ends up at the right edge whatever the title's width. */}
      <div className="h-px flex-1 bg-border" />
      {trailing}
    </div>
  );
}
