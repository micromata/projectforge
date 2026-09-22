import type { ReactNode } from "react";
import { SectionCard } from "@/components/shared/section-card";

interface WizardStepCardProps {
  /** The step's position, spelled as „1." in front of its heading — as Wicket numbers them. */
  number: number;
  heading: string;
  /** What the step is for, from the bundle. */
  intro: string;
  children: ReactNode;
}

/** One numbered step of the wizard: heading, what it does, and the field it does it with. */
export function WizardStepCard({
  number,
  heading,
  intro,
  children,
}: WizardStepCardProps) {
  return (
    <SectionCard className="flex flex-col gap-3">
      <h2 className="text-sm font-semibold">
        {number}. {heading}
      </h2>
      <p className="text-xs text-muted-foreground">{intro}</p>
      {children}
    </SectionCard>
  );
}
