import { cn } from "@/lib/utils";

export interface SectionHeaderProps {
  title: string;
  className?: string;
}

export function SectionHeader({ title, className }: SectionHeaderProps) {
  return (
    <div className={cn("mb-4 flex items-center gap-3.5", className)}>
      <span className="whitespace-nowrap text-[11px] font-bold uppercase tracking-wider text-foreground/70">
        {title}
      </span>
      <div className="h-px flex-1 bg-border" />
    </div>
  );
}
