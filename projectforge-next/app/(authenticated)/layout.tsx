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
      <TwoFactorProvider>{children}</TwoFactorProvider>
    </AuthGuard>
  );
}
