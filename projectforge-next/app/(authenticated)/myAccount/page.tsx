"use client";

import { MyAccountPage } from "@/components/features/my-account/my-account-page";

/**
 * The my-account route (`/next/myAccount`), successor of the React page `react/myAccount/dynamic` (the old
 * url is redirected by the backend's OrphanedLinkFilter). The `<Suspense>` the edit shell's
 * `useSearchParams` needs is the authenticated layout's.
 */
export default function MyAccountRoute() {
  return <MyAccountPage />;
}
