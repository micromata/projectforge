"use client";

import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { BookingInvitation } from "@/components/features/task/booking-invitation/booking-invitation";

/**
 * The invitation link of an ad hoc booking grant (`/next/bookingInvitation?token=…`), mailed to its user by
 * `TaskBookingGrantService`. Behind the login like every page here: only the invited user, logged in, may accept.
 */
export default function BookingInvitationPage() {
  const t = useTranslations();
  const title = t("task.bookingGrant.invitation.title");
  useDocumentTitle(title);

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        {/* `useSearchParams` needs this boundary under `output: "export"`. */}
        <Suspense>
          <BookingInvitation />
        </Suspense>
      </div>
    </PageShell>
  );
}
