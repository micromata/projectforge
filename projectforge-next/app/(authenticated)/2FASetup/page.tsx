"use client";

import { My2FASetupPage } from "@/components/features/my-2fa-setup/my-2fa-setup-page";

/**
 * The 2FA setup route (`/next/2FASetup`), successor of the React pages `react/2FASetup/dynamic` and
 * `react/WebAuthnSetup/dynamic` (the old urls are redirected by the backend's OrphanedLinkFilter).
 */
export default function My2FASetupRoute() {
  return <My2FASetupPage />;
}
