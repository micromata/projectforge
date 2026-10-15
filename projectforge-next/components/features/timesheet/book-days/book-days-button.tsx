"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CalendarAdd02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { todayIso } from "@/lib/date-parse";
import { BookDaysDialog, type BookDaysDialogProps } from "./book-days-dialog";

/**
 * The calendar header's entry to booking several days at once. [getViewStart] is read when the dialog
 * opens, so the period starts in whatever the calendar shows at that moment. With [openOnMount] the
 * dialog opens right away once (e.g. a link from the monthly report), then [onOpened] is called.
 */
export function BookDaysButton({
  getViewStart,
  openOnMount,
  onOpened,
  ...props
}: Omit<BookDaysDialogProps, "viewStart" | "onClose"> & {
  getViewStart: () => string | null;
  openOnMount?: boolean;
  onOpened?: () => void;
}) {
  const t = useTranslations();
  const [viewStart, setViewStart] = useState<string | null>(null);
  const openedOnMount = useRef(false);

  useEffect(() => {
    if (!openOnMount || openedOnMount.current) return;
    openedOnMount.current = true;
    setViewStart(getViewStart() ?? todayIso());
    onOpened?.();
  }, [openOnMount, getViewStart, onOpened]);

  return (
    <>
      <HintTooltip text={t("timesheet.bookDays.title")} plain>
        <Button
          type="button"
          variant="outline"
          size="icon"
          // Framed and in the primary colour, next to the add button: a quieter ghost icon was easy to miss.
          className="size-9 border-primary/60 text-primary hover:bg-primary/10 hover:text-primary"
          aria-label={t("timesheet.bookDays.title")}
          onClick={() => setViewStart(getViewStart() ?? todayIso())}
        >
          <HugeiconsIcon icon={CalendarAdd02Icon} size={20} strokeWidth={2} />
        </Button>
      </HintTooltip>
      {viewStart && (
        <BookDaysDialog
          {...props}
          viewStart={viewStart}
          onClose={() => setViewStart(null)}
        />
      )}
    </>
  );
}
