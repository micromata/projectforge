"use client";

import { notFound, usePathname } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { isRouteDisabled } from "@/lib/feature-routes";

/**
 * Shows "not found" for the pages of switched off features (projectforge.features.*, inactive plugins).
 *
 * The server answers them with 404 on loading (FeatureFilter), but not on navigating inside the app, where the
 * page's code may be loaded already, nor on the dev server.
 */
export function FeatureGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { featureRoutes } = useAuth();
  if (isRouteDisabled(pathname, featureRoutes)) {
    notFound();
  }
  return <>{children}</>;
}
