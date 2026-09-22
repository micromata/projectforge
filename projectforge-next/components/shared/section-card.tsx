import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/**
 * One card of an edit page.
 *
 * While its section is the one the tab bar points at, the whole card is outlined in the accent
 * colour: the card sits in a `group/section` wrapper carrying `data-active` (see EditPageShell), so a
 * section needs to know neither its index nor the scroll position. The later sections can never be
 * scrolled to the top of the column, which makes the card itself the only unambiguous answer to
 * which one was picked.
 */
export function SectionCard({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLElement>) {
  return (
    <section
      className={cn(
        "rounded-md border border-border bg-card px-6 py-5 transition-colors",
        // The outline replaces the existing border rather than adding to it — a ring would shift
        // nothing but would read as a focus state, which the fields inside already use.
        "group-data-[active=true]/section:border-primary/70",
        className
      )}
      {...props}
    >
      {children}
    </section>
  );
}
