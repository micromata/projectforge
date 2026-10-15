import { Suspense } from "react";
import { CalendarShell } from "./calendar-shell";

// The calendar lives in the layout, not the page: it must stay mounted while an edit route
// (calendar/[...edit]) renders beside it as `children`, so a click on an event, a reload of the deep
// link, or a back navigation never tears the calendar down (see CalendarShell).
//
// Suspense because the calendar reads `?gotoDate`/`?hash` (useGotoDate) after a save sends the user
// back here, which under `output: "export"` needs a boundary of its own.
export default function CalendarLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <Suspense>
      <CalendarShell>{children}</CalendarShell>
    </Suspense>
  );
}
