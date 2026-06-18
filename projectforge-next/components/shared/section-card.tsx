import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function SectionCard({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLElement>) {
  return (
    <section
      className={cn(
        "rounded-md border border-border bg-card px-6 py-5",
        className
      )}
      {...props}
    >
      {children}
    </section>
  );
}
