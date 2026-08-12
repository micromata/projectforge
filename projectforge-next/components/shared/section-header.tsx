import { cn } from "@/lib/utils";

export interface SectionHeaderProps {
  title: string;
  className?: string;
}

/**
 * The heading of a section card. Its title takes the accent colour while the section is the active
 * one — the outline around the card is [SectionCard]'s part of that, and the rule inside it stays
 * neutral so the two don't compete.
 */
export function SectionHeader({ title, className }: SectionHeaderProps) {
  return (
    <div className={cn("mb-4 flex items-center gap-3.5", className)}>
      <span className="whitespace-nowrap text-xs font-bold uppercase tracking-wide text-foreground/85 transition-colors group-data-[active=true]/section:text-primary">
        {title}
      </span>
      <div className="h-px flex-1 bg-border" />
    </div>
  );
}
