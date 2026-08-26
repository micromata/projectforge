import { CalendarEditRouteClient } from "./page-client";

// Static export emits one placeholder shell (`calendar/timesheet/new`); Spring forwards every deeper
// /next/calendar/** link to it (see generate-spa-shell-map.mjs and NextSpaResourceResolver), where the
// client reads the real segments from the URL at runtime. The placeholder must be a shape the route
// actually accepts, so the prerender pass renders a valid (empty) modal rather than throwing.
export function generateStaticParams() {
  return [{ edit: ["timesheet", "new"] }];
}

export default function CalendarEditRoutePage() {
  return <CalendarEditRouteClient />;
}
