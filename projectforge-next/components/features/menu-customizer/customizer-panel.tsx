import type { ReactNode } from "react";
import { SectionCard } from "@/components/shared/section-card";

/** One of the two columns of the customizer: a card with a title row and its content. */
export function CustomizerPanel({
  title,
  label,
  actions,
  children,
}: {
  title: string;
  /** A small marker beside the title. */
  label?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <SectionCard className="flex min-w-0 flex-col gap-3 px-4 py-3">
      <div className="flex min-h-7 items-center gap-2">
        <h2 className="text-sm font-semibold">{title}</h2>
        {label && (
          <span className="text-[11px] uppercase tracking-wider text-muted-foreground">
            {label}
          </span>
        )}
        <div className="ml-auto flex min-w-0 items-center gap-2">{actions}</div>
      </div>
      {children}
    </SectionCard>
  );
}
