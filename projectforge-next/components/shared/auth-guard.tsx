"use client";

import { useEffect, useRef } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { Spinner } from "@/components/shared/spinner";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  // Capture the pathname at mount time so subsequent router.replace() calls
  // (which update pathname to "/login") don't re-run the effect and overwrite
  // the returnUrl with "/login".
  const initialPathname = useRef(pathname);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      // The query string belongs to the target as well (e.g. the challenge of the app login, /appLogin?…).
      // Read here and not via useSearchParams, which would need a Suspense boundary above this guard; the
      // address bar still shows the requested url, as this runs before the redirect below.
      const returnUrl = encodeURIComponent(
        initialPathname.current + window.location.search
      );
      router.replace(`/login?returnUrl=${returnUrl}`);
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
