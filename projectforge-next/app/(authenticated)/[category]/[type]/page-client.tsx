"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { DynamicFormPage } from "@/components/dynamic/dynamic-form-page";

/**
 * Renders a server-laid-out create page whose defaults ride the query string rather than an id,
 * e.g. a new timesheet the calendar opens for a slot: `/next/timesheet/edit?startDate=…&firstHour=…`
 * (TimesheetPagesRest reads those from the request). The three-segment sibling handles the id-carrying
 * variant; both share {@link DynamicFormPage}, which reads the query string itself.
 *
 * Category and type are read from the url at runtime because the static export pre-renders a single
 * placeholder route (see page.tsx and use-route-params.ts).
 */
export function DynamicCreatePageClient() {
  // Null while the url doesn't match this route (prerender pass or mid-navigation),
  // which leaves category undefined and disables the query in DynamicFormPage.
  const route = useRouteParams<{
    category: string;
    type: string;
  }>("/[category]/[type]");

  return (
    <DynamicFormPage
      category={route?.category}
      type={route?.type}
      id={undefined}
    />
  );
}
