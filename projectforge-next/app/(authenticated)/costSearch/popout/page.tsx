"use client";

import { CostSearchPopout } from "@/components/shared/cost-search/cost-search-popout";

/**
 * The cost search in a small window of its own (`/next/costSearch/popout`), opened by the floating panel's
 * pop-out button: without header and navigation, to be placed anywhere on the desktop.
 */
export default function CostSearchPopoutRoute() {
  return <CostSearchPopout />;
}
