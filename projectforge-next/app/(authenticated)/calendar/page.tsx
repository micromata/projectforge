// The calendar itself is rendered by the layout so it survives the nested edit routes (see
// calendar/layout.tsx and CalendarShell). The index of `/calendar` therefore adds nothing of its own —
// no edit hangs off it.
export default function CalendarRoutePage() {
  return null;
}
