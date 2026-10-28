"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserSearch01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import { AvailabilityQuickViewDialog } from "./availability-quick-view-dialog";

/**
 * Opens the quick view of a colleague's availability (AvailabilityQuickViewDialog), offered where absences are looked
 * up: the calendar, the vacation account, the vacation and the availability list. `iconOnly` for a crowded toolbar
 * (the calendar's), the text is its tooltip then. The dialog is mounted only while open, so each opening starts with
 * an empty choice.
 */

/** Framed and in the primary colour, as the calendar's BookDaysButton: a quieter ghost button was easy to miss. */
const HIGHLIGHT_CLASS =
  "border-primary/60 text-primary hover:bg-primary/10 hover:text-primary";

export function AvailabilityQuickViewButton({
  iconOnly = false,
  size = "default",
}: {
  iconOnly?: boolean;
  size?: "default" | "sm";
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const label = t("vacation.quickView.button");
  return (
    <>
      {iconOnly ? (
        <HintTooltip text={label} plain>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className={cn("size-9", HIGHLIGHT_CLASS)}
            aria-label={label}
            onClick={() => setOpen(true)}
          >
            <HugeiconsIcon
              icon={UserSearch01Icon}
              size={20}
              strokeWidth={2}
              aria-hidden
            />
          </Button>
        </HintTooltip>
      ) : (
        <Button
          type="button"
          variant="outline"
          size={size}
          className={cn("gap-1.5", HIGHLIGHT_CLASS)}
          onClick={() => setOpen(true)}
        >
          <HugeiconsIcon
            icon={UserSearch01Icon}
            size={size === "sm" ? 14 : undefined}
            aria-hidden
          />
          {label}
        </Button>
      )}
      {open && <AvailabilityQuickViewDialog onClose={() => setOpen(false)} />}
    </>
  );
}
