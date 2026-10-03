"use client";

import { CustomerGroupsPage } from "@/components/features/customer-groups/customer-groups-page";

/**
 * The customer groups route (`/next/customerGroups`): the groups and business units the customer
 * checklists offer. Takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function CustomerGroupsRoute() {
  return <CustomerGroupsPage />;
}
