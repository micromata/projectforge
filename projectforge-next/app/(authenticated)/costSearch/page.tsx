"use client";

import { CostSearchPage } from "@/components/shared/cost-search/cost-search-page";

/**
 * The cost search (`/next/costSearch`), the successor of the React page `react/costSearch/dynamic`. The same
 * lookup floats over every page (top bar button, `ALT-K`) and opens in a window of its own (`popout`).
 */
export default function CostSearchRoute() {
  return <CostSearchPage />;
}
