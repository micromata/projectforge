import { Suspense } from "react";
import { SecretOfferAlerts } from "@/components/features/chat/secret-offer-alerts";
import { AuthGuard } from "@/components/shared/auth-guard";
import { CostSearchPanelHost } from "@/components/shared/cost-search/cost-search-panel-host";
import { FeatureGuard } from "@/components/shared/feature-guard";
import { EntityEditModalHost } from "@/components/shared/edit/entity-edit-modal-host";
import { JobToasts } from "@/components/shared/jobs/job-toasts";
import { NotificationToasts } from "@/components/shared/notifications/notification-toasts";
import { TwoFactorProvider } from "@/components/shared/two-factor-provider";
import { UnsavedChangesDialogHost } from "@/components/shared/unsaved-changes-dialog-host";
import { UserEvents } from "@/components/shared/user-events";

export default function AuthenticatedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      {/* Protected actions may demand a (new) second factor at any time. */}
      {/* Here and not on the page starting a job: a re-index outlives the list page it was started from. */}
      <JobToasts />
      {/* The user's notifications shown as toasts, on whatever page the poll finds them. */}
      <NotificationToasts />
      {/* One event stream of the user for every page; it wakes e.g. the signals of a confidential
          conversation offered, which has to reach the user wherever the app is open. */}
      <UserEvents />
      <SecretOfferAlerts />
      {/* The one edit modal any page below can open (the calendar's timesheets and team events, the
          structure wizard's groups) — mounted here so it outlives whatever raised it. */}
      <EntityEditModalHost />
      {/* The app's own "leave unsaved changes?" question, in place of the browser's confirm box — one
          instance any link or modal below raises (see confirmLeaveUnsavedChanges). */}
      <UnsavedChangesDialogHost />
      {/* The floating cost search finance users keep open while working on any page — here, so it stays
          where it is when the page below changes. */}
      <CostSearchPanelHost />
      {/* Anything below may read the query string, which under `output: "export"` needs a boundary of
          its own: a page calling `useSearchParams()` without one fails the build. Stated once here
          rather than per page — the pages below are client rendered anyway, so the bailout it causes
          costs nothing (see useListSelection, which reads `?multiSelectionMode=true`). */}
      <Suspense fallback={null}>
        <TwoFactorProvider>
          {/* The pages of switched off features are "not found", also on navigating inside the app. */}
          <FeatureGuard>{children}</FeatureGuard>
        </TwoFactorProvider>
      </Suspense>
    </AuthGuard>
  );
}
