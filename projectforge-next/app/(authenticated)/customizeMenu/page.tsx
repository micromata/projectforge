"use client";

import { MenuCustomizerPage } from "@/components/features/menu-customizer/menu-customizer-page";

/**
 * "Customize my menu" (`/next/customizeMenu`), successor of the old React page `react/customizeMenu`.
 *
 * No `<Suspense>` boundary is needed: this page reads no `useSearchParams`.
 */
export default function CustomizeMenuRoute() {
  return <MenuCustomizerPage />;
}
