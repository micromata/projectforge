"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { PageShell } from "@/components/shared/page-shell";

/**
 * The start page (/next) has no content of its own; it forwards to the calendar.
 *
 * A client-side replace is used on purpose: prod ships as a static export with no
 * server, so a runtime `redirect()` is not available. `replace` (not `push`) keeps
 * the empty start page out of the history, and the basePath (/next) is prepended
 * automatically, so the target is the bare "/calendar".
 */
export function HomePageClient() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/calendar");
  }, [router]);

  return <PageShell>{null}</PageShell>;
}
