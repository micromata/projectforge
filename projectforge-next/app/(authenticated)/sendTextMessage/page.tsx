"use client";

import { Suspense } from "react";
import { SendTextMessagePage } from "@/components/features/send-text-message/send-text-message-page";

/**
 * The "Send text message" page (`/next/sendTextMessage`), successor of Wicket's `wa/sendSms`.
 *
 * The `<Suspense>` boundary is required because the page reads the optional deep-link
 * `?addressId=&phoneType=&number=` via `useSearchParams` under the static export (`output: "export"`),
 * same as the search and monthly-report pages.
 */
export default function SendTextMessageRoute() {
  return (
    <Suspense>
      <SendTextMessagePage />
    </Suspense>
  );
}
