"use client";

import { ChangePasswordPage } from "@/components/features/change-password/change-password-page";

/**
 * The route `/next/changePassword`, successor of the React page `react/changePassword/dynamic` (the old url is
 * redirected by the backend's OrphanedLinkFilter). The `<Suspense>` the page's `useSearchParams` needs is
 * the authenticated layout's.
 */
export default function ChangePasswordRoute() {
  return <ChangePasswordPage variant="password" />;
}
