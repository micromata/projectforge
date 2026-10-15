"use client";

import { Suspense } from "react";
import { PhoneCallPage } from "@/components/features/phone-call/phone-call-page";

/**
 * The "Direct call" page (`/next/phoneCall`), successor of Wicket's `wa/phoneCall`.
 *
 * The `<Suspense>` boundary is required because the page reads the optional deep-link
 * `?addressId=&number=&callerPage=` via `useSearchParams` under the static export (`output: "export"`),
 * same as the send-text-message and search pages.
 */
export default function PhoneCallRoute() {
  return (
    <Suspense>
      <PhoneCallPage />
    </Suspense>
  );
}
