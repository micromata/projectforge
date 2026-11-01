"use client";

import { AppLoginPage } from "@/components/features/app-login/app-login-page";

/**
 * Connecting the ProjectForge app (`/next/appLogin`, AppAuthRest), opened by the app in the system browser.
 * Not in any menu.
 */
export default function AppLoginRoute() {
  return <AppLoginPage />;
}
