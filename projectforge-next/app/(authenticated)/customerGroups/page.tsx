"use client";

import { CustomerGroupsPage } from "@/components/features/customer-groups/customer-groups-page";

/**
 * The customer groups route (`/next/customerGroups`): the groups and business units the customer
 * checklists offer. `?returnTo=` names the page cancelling leads back to (see CustomerGroupsForm); the
 * `<Suspense>` `useSearchParams` needs is the authenticated layout's.
 */
export default function CustomerGroupsRoute() {
  return <CustomerGroupsPage />;
}
