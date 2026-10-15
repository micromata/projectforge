"use client";

import { FeedbackPage } from "@/components/features/feedback/feedback-page";

/**
 * The "Send feedback" page (`/next/feedback`), successor of Wicket's `wa/feedback`.
 *
 * No `<Suspense>` boundary is needed (unlike the SMS page): this page reads no `useSearchParams`.
 */
export default function FeedbackRoute() {
  return <FeedbackPage />;
}
