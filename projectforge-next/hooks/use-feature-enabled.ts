"use client";

import { useAuth } from "@/hooks/use-auth";

/**
 * Whether the feature with this id (a menu id of the backend, e.g. `SEARCH`, see FeatureService) is switched on.
 * For what the app shows on every page, not reached through the menu (the menu comes without the switched off
 * entries anyway).
 */
export function useFeatureEnabled(id: string): boolean {
  const { disabledFeatures } = useAuth();
  return !disabledFeatures.includes(id);
}
