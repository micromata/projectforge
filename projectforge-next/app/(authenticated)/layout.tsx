import { AuthGuard } from "@/components/shared/auth-guard";
import { TwoFactorProvider } from "@/components/shared/two-factor-provider";

export default function AuthenticatedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      {/* Protected actions may demand a (new) second factor at any time. */}
      <TwoFactorProvider>{children}</TwoFactorProvider>
    </AuthGuard>
  );
}
