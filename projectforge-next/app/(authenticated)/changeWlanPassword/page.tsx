"use client";

import { ChangePasswordPage } from "@/components/features/change-password/change-password-page";

/**
 * The route `/next/changeWlanPassword`, successor of the React page `react/changeWlanPassword/dynamic` (the old url is
 * redirected by the backend's OrphanedLinkFilter). The `<Suspense>` the page's `useSearchParams` needs is
 * the authenticated layout's.
 */
export default function ChangeWlanPasswordRoute() {
  return <ChangePasswordPage variant="wlan" />;
}
