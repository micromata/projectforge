import { Suspense } from "react";
import { AuthGuard } from "@/components/shared/auth-guard";
import { JobToasts } from "@/components/shared/jobs/job-toasts";
import { TwoFactorProvider } from "@/components/shared/two-factor-provider";

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
      {/* Anything below may read the query string, which under `output: "export"` needs a boundary of
          its own: a page calling `useSearchParams()` without one fails the build. Stated once here
          rather than per page — the pages below are client rendered anyway, so the bailout it causes
          costs nothing (see useListSelection, which reads `?multiSelectionMode=true`). */}
      <Suspense fallback={null}>
        <TwoFactorProvider>{children}</TwoFactorProvider>
      </Suspense>
    </AuthGuard>
  );
}
