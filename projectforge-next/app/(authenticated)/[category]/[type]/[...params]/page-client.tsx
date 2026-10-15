"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { DynamicFormPage } from "@/components/dynamic/dynamic-form-page";

/**
 * Renders any server-laid-out edit page carrying an id, e.g. `/next/address/edit/42`.
 *
 * Category, type and id are read from the url at runtime, because the static export cannot
 * pre-render one file per entity (see page.tsx and use-route-params.ts). The actual rendering
 * is shared with the two-segment route via {@link DynamicFormPage}.
 */
export function DynamicFormPageClient() {
  // Null while the url doesn't match this route (prerender pass or mid-navigation),
  // which leaves category undefined and disables the query in DynamicFormPage.
  const route = useRouteParams<{
    category: string;
    type: string;
    params: string[];
  }>("/[category]/[type]/[...params]");

  return (
    <DynamicFormPage
      category={route?.category}
      type={route?.type}
      id={route?.params?.[0]}
    />
  );
}
