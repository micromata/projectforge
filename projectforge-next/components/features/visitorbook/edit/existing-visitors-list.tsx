"use client";

import { useTranslations } from "next-intl";
import { PopoverContent } from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type { ExistingVisitor } from "@/lib/rs/visitorbook";
import { cn } from "@/lib/utils";

/**
 * The stored visitors offered below the name of a new one (see VisitorbookNameField): name, company and the latest
 * visit, each a row to pick. Rendered inside the field's Popover, anchored at its input.
 */
export function ExistingVisitorsList({
  visitors,
  activeIndex,
  onActivate,
  onPick,
}: {
  visitors: readonly ExistingVisitor[];
  /** The row the arrow keys have moved to, `-1` for none. */
  activeIndex: number;
  onActivate: (index: number) => void;
  onPick: (visitor: ExistingVisitor) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const describe = (visitor: ExistingVisitor) =>
    visitor.lastDateOfVisit
      ? t("orga.visitorbook.existing.lastVisit", {
          arg0: formatDate(visitor.lastDateOfVisit, ctx),
          arg1: visitor.numberOfVisits,
        })
      : t("orga.visitorbook.existing.noVisit");

  return (
    <PopoverContent
      align="start"
      className="w-auto max-w-md min-w-(--radix-popover-trigger-width) p-1"
      role="listbox"
      // The caret stays in the input, which is outside this content: closing is the input's onBlur.
      onOpenAutoFocus={(e) => e.preventDefault()}
      onFocusOutside={(e) => e.preventDefault()}
    >
      <p className="px-2 py-1 text-xs text-muted-foreground">
        {t("orga.visitorbook.existing.hint")}
      </p>
      {visitors.map((visitor, index) => (
        <button
          key={visitor.id}
          type="button"
          role="option"
          aria-selected={index === activeIndex}
          className={cn(
            "block w-full rounded px-2 py-1 text-left text-sm hover:bg-muted",
            index === activeIndex && "bg-muted"
          )}
          // Keeps the focus in the input, so its onBlur doesn't close the list before the click lands.
          onMouseDown={(e) => e.preventDefault()}
          onMouseEnter={() => onActivate(index)}
          onClick={() => onPick(visitor)}
        >
          <span className="font-medium">
            {[visitor.lastname, visitor.firstname].filter(Boolean).join(", ")}
          </span>
          {visitor.company && (
            <span className="text-muted-foreground"> · {visitor.company}</span>
          )}
          <span className="block text-xs text-muted-foreground">
            {describe(visitor)}
          </span>
        </button>
      ))}
    </PopoverContent>
  );
}
