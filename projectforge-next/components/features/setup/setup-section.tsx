"use client";

import type { ReactNode } from "react";

interface SetupSectionProps {
  title: string;
  /** One short sentence under the title. */
  description?: string;
  children: ReactNode;
}

/** A titled group of fields on the setup page; its fields sit in two columns from the `sm` breakpoint on. */
export function SetupSection({
  title,
  description,
  children,
}: SetupSectionProps) {
  return (
    <section className="grid gap-4 rounded-lg border p-4">
      <div className="grid gap-0.5">
        <h2 className="text-sm font-medium">{title}</h2>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      <div className="grid gap-4 sm:grid-cols-2">{children}</div>
    </section>
  );
}
